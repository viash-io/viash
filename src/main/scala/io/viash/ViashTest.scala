/*
 * Copyright (C) 2020  Data Intuitive
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.viash

import sys.process.{Process, ProcessLogger}
import java.io.{ByteArrayOutputStream, FileWriter, PrintWriter}
import java.nio.file.{Files, Path, Paths}
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import scala.util.Random

import config.Config
import functionality.Functionality
import functionality.arguments.{FileArgument, Output}
import functionality.resources.{BashScript, Script}
import platforms.NativePlatform
import helpers.IO
import io.viash.helpers.data_structures._
import io.viash.helpers.MissingResourceFileException

object ViashTest {
  case class TestOutput(name: String, exitValue: Int, output: String, logFile: String, duration: Long)
  case class ManyTestOutput(setup: Option[TestOutput], tests: List[TestOutput])

  def apply(
    config: Config,
    keepFiles: Option[Boolean] = None,
    quiet: Boolean = false,
    setupStrategy: String = "cachedbuild",
    tempVersion: Boolean = true,
    verbosityLevel: Int = 6,
    parentTempPath: Option[Path] = None, 
    cpus: Option[Int], 
    memory: Option[String]
  ): ManyTestOutput = {
    // create temporary directory
    val dir = IO.makeTemp("viash_test_" + config.functionality.name, parentTempPath)
    if (!quiet) println(s"Running tests in temporary directory: '$dir'")

    // set version to temporary value
    val config2 = if (tempVersion) {
      config.copy(
        functionality = config.functionality.copy(
          version = Some(Random.alphanumeric.take(12).mkString)
        )
      )
    } else {
      config
    }

    // run tests
    val ManyTestOutput(setupRes, results) = ViashTest.runTests(
      config = config2,
      dir = dir,
      verbose = !quiet,
      setupStrategy = setupStrategy,
      verbosityLevel = verbosityLevel,
      cpus = cpus,
      memory = memory
    )
    val count = results.count(_.exitValue == 0)
    val anyErrors = setupRes.exists(_.exitValue > 0) || count < results.length

    val errorMessage =
      if (!anyErrors) {
        ""
      } else if (setupRes.exists(_.exitValue > 0)) {
        "Setup failed!"
      } else {
        s"Only $count out of ${results.length} test scripts succeeded!"
      }

    if (!quiet) {
      if (results.isEmpty && !anyErrors) {
        println(s"${Console.RED}WARNING! No tests found!${Console.RESET}")
      } else if (anyErrors) {
        println(s"${Console.RED}ERROR! $errorMessage${Console.RESET}")
      } else {
        println(s"${Console.GREEN}SUCCESS! All $count out of ${results.length} test scripts succeeded!${Console.RESET}")
      }
    }

    // keep temp files if user asks or any errors are encountered

    if (!keepFiles.getOrElse(anyErrors)) {
      if (!quiet) println("Cleaning up temporary directory")
      IO.deleteRecursively(dir)
    }
    // TODO: remove container

    if (anyErrors && !quiet) {
      throw new RuntimeException(errorMessage)
    }

    ManyTestOutput(setupRes, results)
  }

  def runTests(config: Config, dir: Path, verbose: Boolean = true, setupStrategy: String, verbosityLevel: Int, cpus: Option[Int], memory: Option[String]): ManyTestOutput = {
    val fun = config.functionality
    val platform = config.platform.get

    val consoleLine = "===================================================================="

    // build regular executable
    val configWithReqs = config.copy(
      config.functionality.copy(
        requirements = config.functionality.requirements.copy(
          cpus = if (cpus.isDefined) cpus else config.functionality.requirements.cpus,
          memory = if (memory.isDefined) memory else config.functionality.requirements.memory
        )
      )
    )
    val buildFun = platform.modifyFunctionality(configWithReqs, true)
    val buildDir = dir.resolve("build_executable")
    Files.createDirectories(buildDir)
    try {
      IO.writeResources(buildFun.resources, buildDir)
    } catch {
      case e: MissingResourceFileException =>
        // add config file name to the exception and throw again
        if (config.info.isDefined && e.config == "") {
          throw MissingResourceFileException(e.resource, Some(config.info.get.config), cause= e.cause)
        }
        throw e
    }

    // run command, collect output
    val buildResult =
      if (!platform.hasSetup) {
        None
      } else {
        // todo: setupStrategy will have to be handled differently when non-docker platforms need setting up.
        val stream = new ByteArrayOutputStream
        val printWriter = new PrintWriter(stream)
        val logPath = Paths.get(buildDir.toString, "_viash_build_log.txt").toString
        val logWriter = new FileWriter(logPath, true)

        val logger: String => Unit =
          (s: String) => {
            if (verbose) println(s)
            printWriter.println(s)
            logWriter.append(s + sys.props("line.separator"))
          }

        logger(consoleLine)

        // run command, collect output
        try {
          val executable = Paths.get(buildDir.toString, fun.name).toString
          val cmd = Seq(executable, "---verbosity", verbosityLevel.toString, "---setup", setupStrategy)
          logger("+" + cmd.mkString(" "))
          val startTime = LocalDateTime.now

          // create tempdir in test
          val subTmp = Paths.get(buildDir.toString, "tmp")
          Files.createDirectories(subTmp)

          val exitValue = Process(
            cmd,
            cwd = buildDir.toFile,
            ("VIASH_TEMP", subTmp.toString())
          ).!(ProcessLogger(logger, logger))
          val endTime = LocalDateTime.now
          val diffTime = ChronoUnit.SECONDS.between(startTime, endTime)
          printWriter.flush()
          Some(TestOutput("build_executable", exitValue, stream.toString, logPath, diffTime))
        } finally {
          printWriter.close()
          logWriter.close()
        }
      }

    // if setup failed, return faster
    if (buildResult.exists(_.exitValue > 0)) {
      return ManyTestOutput(buildResult, Nil)
    }

    // generate executable for native platform
    val exe = NativePlatform().modifyFunctionality(config, true).resources.head

    // fetch tests
    val tests = fun.test_resources

    val testResults = tests.filter(_.isInstanceOf[Script]).map {
      case test: Script if test.read.isEmpty =>
        TestOutput(test.filename, 1, "Test script does not exist.", "", 0L)

      case test: Script =>
        val startTime = LocalDateTime.now

        // make a new directory
        val dirName = "test_" + test.filename.replaceAll("\\.[^\\.]*$", "")
        val newDir = dir.resolve(dirName)
        Files.createDirectories(newDir)
        val dirArg = FileArgument(
          name = "dir",
          direction = Output,
          default = OneOrMore(dir)
        )

        // generate bash script for test
        val funOnlyTest = platform.modifyFunctionality(
          config.copy(
            functionality = Functionality(
              // set same name, namespace and version
              // to be able to reuse same docker container
              name = config.functionality.name,
              namespace = config.functionality.namespace,
              version = config.functionality.version,
              // set dirArg as argument so that Docker can chown it after execution
              arguments = List(dirArg),
              argument_groups = Nil,
              resources = List(test),
              set_wd_to_resources_dir = true
            )
          ), 
          testing = true
        )
        val testBash = BashScript(
          dest = Some("test_executable"),
          text = funOnlyTest.resources.head.text
        )

        // assemble full resources list for test
        val funFinal = fun.copy(resources = 
          testBash :: // the test, wrapped in a bash script
            exe :: // the executable, wrapped with a native platform,
            // to be run inside of the platform of the test
            funOnlyTest.additionalResources ::: // other resources generated by wrapping the test script
            fun.additionalResources ::: // other resources provided in fun.resources
            tests.filter(!_.isInstanceOf[Script]) // other resources provided in fun.tests
        )

        // write resources to dir
        IO.writeResources(funFinal.resources, newDir)

        // run command, collect output
        val stream = new ByteArrayOutputStream
        val printWriter = new PrintWriter(stream)
        val logPath = Paths.get(newDir.toString, "_viash_test_log.txt").toString
        val logWriter = new FileWriter(logPath, true)

        val logger: String => Unit =
          (s: String) => {
            if (verbose) println(s)
            printWriter.println(s)
            logWriter.append(s + sys.props("line.separator"))
          }

        logger(consoleLine)

        try {
          // run command, collect output
          val executable = Paths.get(newDir.toString, testBash.filename).toString

          // create tempdir in test
          val subTmp = Paths.get(newDir.toString, "tmp")
          Files.createDirectories(subTmp)

          val cmd = Seq(executable) ++ Seq(cpus.map("---cpus=" + _), memory.map("---memory="+_)).flatMap(a => a)
          logger(s"+${cmd.mkString(" ")}")
          val exitValue = Process(
            cmd,
            cwd = newDir.toFile,
            "VIASH_TEMP" -> subTmp.toString
          ).!(ProcessLogger(logger, logger))

          printWriter.flush()

          val endTime = LocalDateTime.now
          val diffTime = ChronoUnit.SECONDS.between(startTime, endTime)
          TestOutput(test.filename, exitValue, stream.toString, logPath, diffTime)
        } finally {
          printWriter.close()
          logWriter.close()
        }
    }

    if (verbose) println(consoleLine)

    ManyTestOutput(buildResult, testResults)
  }
}
