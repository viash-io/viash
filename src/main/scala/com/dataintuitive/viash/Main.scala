package com.dataintuitive.viash

import functionality._
import targets._
import resources.Script

import java.nio.file.{Paths, Files}
import scala.io.Source
import org.rogach.scallop.{Subcommand, ScallopOption}

import sys.process._
import com.dataintuitive.viash.helpers.{Exec, IOHelper}
import com.dataintuitive.viash.functionality.resources.Resource

object Main {
  def main(args: Array[String]) {
    val (viashArgs, runArgs) = args.span(_ != "--")

    val conf = new CLIConf(viashArgs)

    if (conf.version.getOrElse(false)) {
      val p = getClass.getPackage
      val name = p.getImplementationTitle
      val version = p.getImplementationVersion
      println(name + " v" + version)
      System.exit(0)
    }

    conf.subcommand match {
      case Some(conf.run) => {
        // create new functionality with argparsed executable
        val (fun, tar) = viashLogic(conf.run)

        // make temporary directory
        val dir = IOHelper.makeTemp("viash_" + fun.name)

        try {
          // write executable and resources to temporary directory
          writeResources(fun.resources, dir)

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
      case Some(conf.export) => {
        // create new functionality with argparsed executable
        val (fun, tar) = viashLogic(conf.export)

        // write files to given output directory
        val dir = new java.io.File(conf.export.output())
        dir.mkdirs()
        writeResources(fun.resources, dir)

        if (conf.export.meta()) {
          val execPath = Paths.get(dir.toString(), fun.mainScript.get.filename).toString()
          println(s"""functionalityPath: ${conf.export.functionality()}
            |platformPath: ${conf.export.platform.getOrElse("")}
            |outputPath: ${conf.export.output()}
            |executablePath: $execPath""".stripMargin)
        }
      }
      case Some(conf.pimp) => {
        // read functionality
        implicit val functionality = readFunctionality(conf.pimp.functionality)

        // fetch argparsed code
        val mainScript = functionality.mainScript.get

        // write to file or stdout
        if (conf.pimp.output.isDefined) {
          val path = Paths.get(conf.pimp.output())
          mainScript.writeWithArgparse(path, true)
        } else {
          println(mainScript.readWithArgparse)
        }
      }
      case Some(conf.test) => {
        val fun = readFunctionality(conf.test.functionality)
        val platform = readPlatform(conf.test.platform)
        val verbose = conf.test.verbose()

        // create temporary directory
        val dir = IOHelper.makeTemp("viash_test_" + fun.name)

        val results = ViashTester.runTests(fun, platform, dir, verbose = verbose)

        val code = ViashTester.reportTests(results, dir, verbose = verbose)

        if (!conf.test.keep() && !results.exists(_.exitValue > 0)) {
          println("Cleaning up temporary files")
          IOHelper.deleteRecursively(dir)
        } else {
          println(s"Test files and logs are stored at '$dir'")
        }

        System.exit(code)
      }
      case _ => println("No subcommand was specified. See `viash --help` for more information.")
    }
  }

  def readFunctionality(opt: ScallopOption[String]) = {
    val funcPath = new java.io.File(opt()).getAbsoluteFile()
    val functionality = Functionality.parse(funcPath)
    functionality
  }

  def readPlatform(opt: ScallopOption[String]) = {
    opt.map{ path =>
      val targPath = new java.io.File(path)
      Target.parse(targPath)
    }.getOrElse(NativeTarget())
  }

  def viashLogic(subcommand: WithFunctionality with WithPlatform) = {
    // get the functionality yaml
    // let the functionality object know the path in which it resided,
    // so it can find back its resources
    val functionality = readFunctionality(subcommand.functionality)

    // get the platform
    // if no platform is provided, assume the platform
    // should be native and all dependencies are taken care of
    val platform = readPlatform(subcommand.platform)

    // modify the functionality using the target
    val fun2 = platform.modifyFunctionality(functionality)

    (fun2, platform)
  }

  def writeResources(
    resources: Seq[Resource],
    outputDir: java.io.File,
    overwrite: Boolean = true
  ) {
    // copy all files
    resources.foreach{ resource =>
      val dest = Paths.get(outputDir.getAbsolutePath, resource.filename)
      resource.write(dest, overwrite)
    }
  }


}
