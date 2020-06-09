package com.dataintuitive.viash

import functionality._
import targets._
import resources.Script

import java.nio.file.{Paths, Files}
import scala.io.Source
import org.rogach.scallop.Subcommand

import java.nio.charset.StandardCharsets

import sys.process._
import com.dataintuitive.viash.helpers.Exec
import com.dataintuitive.viash.functionality.resources.Resource

object Main {
  def main(args: Array[String]) {
    val (viashArgs, runArgs) = args.span(_ != "--")

    val conf = new CLIConf(viashArgs)

    conf.subcommand match {
      case Some(conf.run) => {
        // create new functionality with argparsed executable
        val (fun, tar) = viashLogic(conf.run, None)

        // write executable and resources to temporary directory
        val dir = Files.createTempDirectory("viash_" + fun.name).toFile()
        writeResources(fun.resources, fun.rootDir, dir)

        // execute with parameters
        val executable = Paths.get(dir.toString(), fun.name).toString()
        println(Exec.run(
          Array(executable) ++
          runArgs.dropWhile(_ == "--")
        ))
      }
      case Some(conf.export) => {
        // create new functionality with argparsed executable
        val (fun, tar) = viashLogic(conf.export, None)

        // write files to given output directory
        val dir = new java.io.File(conf.export.output())
        dir.mkdirs()
        writeResources(fun.resources, fun.rootDir, dir)
      }
      case Some(conf.pimp) => {
        // read functionality
        val functionality = readFunctionality(conf.pimp.functionality())

        // fetch argparsed code
        val mainCode = functionality.mainCodeWithArgParse.get

        // write to file or stdout
        if (conf.pimp.output.isDefined) {
          val file = new java.io.File(conf.pimp.output())
          Files.write(file.toPath(), mainCode.getBytes(StandardCharsets.UTF_8))
          file.setExecutable(true)
        } else {
          println(mainCode)
        }
      }
      case Some(conf.test) => {
        val fun = readFunctionality(conf.test.functionality())
        val platform = conf.test.platform.map{ path =>
          val targPath = new java.io.File(path)
          Target.parse(targPath)
        }.getOrElse(NativeTarget())

        val results = ViashTester.testFunctionality(fun, platform)

        if (results.length == 0) {
          println("No tests found!")
        } else {
          println()

          for ((filename, code, stdout) ← results if code > 0 || conf.test.verbose()) {
            println(s">> $filename finished with code $code:")
            println(stdout)
            println()
          }

          val count = results.count(_._2 != 0)

          if (count > 0) {
            println(s"$count out of ${results.length} test scripts failed!")
            println("Check the output above for more information.")
            System.exit(1)
          } else {
            println("All test scripts succeeded!")
          }
        }
      }
      case _ => println("No subcommand was specified. See `viash --help` for more information.")
    }
  }

  def readFunctionality(funStr: String) = {
    val funcPath = new java.io.File(funStr).getAbsoluteFile()
    val functionality = Functionality.parse(funcPath)
    functionality.rootDir = funcPath
    functionality
  }

  def viashLogic(subcommand: WithFunctionality with WithPlatform, test: Option[Script]) = {
    // get the functionality yaml
    // let the functionality object know the path in which it resided,
    // so it can find back its resources
    val functionality = readFunctionality(subcommand.functionality())

    // get the platform
    // if no platform is provided, assume the platform
    // should be native and all dependencies are taken care of
    val platform = subcommand.platform.map{ path =>
      val targPath = new java.io.File(path)
      Target.parse(targPath)
    }.getOrElse(NativeTarget())

    // modify the functionality using the target
    val fun2 = platform.modifyFunctionality(functionality)

    (fun2, platform)
  }

  def writeResources(
    resources: Seq[Resource],
    inputDir: java.io.File,
    outputDir: java.io.File,
    overwrite: Boolean = true
  ) {
    // copy all files
    resources.foreach(
      resource => {
        val dest = Paths.get(outputDir.getAbsolutePath, resource.filename)

        val destFile = dest.toFile()
        if (overwrite && destFile.exists()) {
          destFile.delete()
        }

        if (resource.path.isDefined) {
          val sour = Paths.get(inputDir.getPath(), resource.path.get)
          Files.copy(sour, dest)
        } else {
          val text = resource.text.get
          Files.write(dest, text.getBytes(StandardCharsets.UTF_8))
        }

        destFile.setExecutable(resource.is_executable)
      }
    )
  }
}
