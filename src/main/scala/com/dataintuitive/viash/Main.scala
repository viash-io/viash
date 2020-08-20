package com.dataintuitive.viash

import functionality._
import platforms._
import resources._
import helpers.{Exec, IOHelper}
import config.Config

import java.nio.file.{Paths, Files, Path}
import scala.io.Source
import org.rogach.scallop.{Subcommand, ScallopOption}

import sys.process._

object Main {
  private val pkg = getClass.getPackage
  val name = pkg.getImplementationTitle
  val version = pkg.getImplementationVersion

  def main(args: Array[String]) {
    val (viashArgs, runArgs) = args.span(_ != "--")

    val conf = new CLIConf(viashArgs)

    conf.subcommands match {
      case List(conf.run) => {
        // create new functionality with argparsed executable
        val config = readAll(conf.run)
        val fun = config.functionality

        // make temporary directory
        val dir = IOHelper.makeTemp("viash_" + fun.name)

        try {
          // write executable and resources to temporary directory
          IOHelper.writeResources(fun.resources.getOrElse(Nil), dir)

          // determine command
          val cmd =
            Array(Paths.get(dir.toString(), fun.name).toString()) ++
            runArgs.dropWhile(_ == "--")

          // execute command, print everything to console
          val code = Process(cmd).!(ProcessLogger(println, println))
          System.exit(code)
        } finally {
          // always remove tempdir afterwards
          if (!conf.run.keep()) {
            IOHelper.deleteRecursively(dir)
          } else {
            println(s"Files and logs are stored at '$dir'")
          }
        }
      }
      case List(conf.export) => {
        val config = readAll(conf.export)
        val outputPath = conf.export.output()
        val config2 = config.copy(
          info = config.info.map(_.copy(
            output_path = Some(outputPath)
          ))
        )
        ViashExport.export(config2, outputPath, conf.export.meta())
      }
      case List(conf.test) => {
        val config = readAll(conf.test, modifyFun = false)
        val fun = config.functionality
        val plat = config.platform.get

        val verbose = conf.test.verbose()

        // create temporary directory
        val dir = IOHelper.makeTemp("viash_test_" + fun.name)

        val results = ViashTester.runTests(fun, plat, dir, verbose = verbose)

        val code = ViashTester.reportTests(results, dir, verbose = verbose)

        if (!conf.test.keep() && !results.exists(_.exitValue > 0)) {
          println("Cleaning up temporary files")
          IOHelper.deleteRecursively(dir)
        } else {
          println(s"Test files and logs are stored at '$dir'")
        }

        System.exit(code)
      }
      case List(conf.namespace, conf.namespace.build) => {
        val namespace = conf.namespace.build.namespace()
        val source = conf.namespace.build.src.getOrElse(Paths.get("src", namespace).toString)
        val target = conf.namespace.build.target.getOrElse(Paths.get("target/").toString)

        ViashNamespace.build(namespace, source, target)
      }
      case _ => println("No subcommand was specified. See `viash --help` for more information.")
    }
  }

  def readAll(
    subcommand: ViashCommand,
    modifyFun: Boolean = true
  ): Config = {
    Config.read(
      component = subcommand.component.toOption,
      functionality = subcommand.functionality.toOption,
      platform = subcommand.platform.toOption,
      platformID = subcommand.platformID.toOption,
      modifyFun = modifyFun
    )
  }
}
