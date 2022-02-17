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

package com.dataintuitive.viash

import functionality._
import dataobjects.{FileObject, Output}
import platforms._
import resources.{BashScript, Script}

import sys.process.{Process, ProcessLogger}
import java.io.{ByteArrayOutputStream, File, FileWriter, PrintWriter}
import java.nio.file.{Files, Path, Paths}
import com.dataintuitive.viash.config.{Config, Version}
import helpers.IO

import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import scala.util.Random

object ViashTest {
  case class TestOutput(name: String, exitValue: Int, output: String, logFile: String, duration: Long)
  case class ManyTestOutput(setup: Option[TestOutput], tests: List[TestOutput])

  def apply(
    config: Config,
    keepFiles: Option[Boolean] = None,
    quiet: Boolean = false,
    setupStrategy: String = "cachedbuild",
    tempVersion: Boolean = true,
    verbosityLevel: Int = 6
  ): ManyTestOutput = {
    // create temporary directory
    val dir = IO.makeTemp("viash_test_" + config.functionality.name)
    if (!quiet) println(s"Running tests in temporary directory: '$dir'")

    // set version to temporary value
    val config2 = if (tempVersion) {
      config.copy(
        functionality = config.functionality.copy(
          version = Some(Version(Random.alphanumeric.take(12).mkString))
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
      verbosityLevel = verbosityLevel
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

    if (anyErrors && !quiet) {
      throw new RuntimeException(errorMessage)
    }

    ManyTestOutput(setupRes, results)
  }

  def runTests(config: Config, dir: Path, verbose: Boolean = true, setupStrategy: String, verbosityLevel: Int): ManyTestOutput = {
    val fun = config.functionality
    val platform = config.platform.get

    val consoleLine = "===================================================================="

    // build regular executable
    val buildFun = platform.modifyFunctionality(fun)
    val buildDir = dir.resolve("build_executable")
    Files.createDirectories(buildDir)
    IO.writeResources(buildFun.resources.getOrElse(Nil), buildDir)

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
          logger(s"+$executable ---verbosity $verbosityLevel ---setup $setupStrategy")
          val startTime = LocalDateTime.now
          val exitValue = Process(Seq(executable, "---verbosity", verbosityLevel.toString, "---setup", setupStrategy), cwd = buildDir.toFile).!(ProcessLogger(logger, logger))
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
    val exe = NativePlatform().modifyFunctionality(fun).resources.get.head

    // fetch tests
    val tests = fun.tests.getOrElse(Nil)

    val testResults = tests.filter(_.isInstanceOf[Script]).map {
      case test: Script if test.read.isEmpty =>
        TestOutput(test.filename, 1, "Test script does not exist.", "", 0L)

      case test: Script =>
        val startTime = LocalDateTime.now
        val dirArg = FileObject(
          name = "dir",
          direction = Output,
          default = Some(dir)
        )
        // generate bash script for test
        val funOnlyTest = platform.modifyFunctionality(fun.copy(
          arguments = Nil,
          dummy_arguments = Some(List(dirArg)),
          resources = Some(List(test)),
          set_wd_to_resources_dir = true,
          add_resources_to_path = true
        ))
        val testBash = BashScript(
          dest = Some(test.filename),
          text = funOnlyTest.resources.getOrElse(Nil).head.text
        )

        // assemble full resources list for test
        val funFinal = fun.copy(resources = Some(
          testBash :: // the test, wrapped in a bash script
            exe :: // the executable, wrapped with a native platform,
            // to be run inside of the platform of the test
            funOnlyTest.resources.getOrElse(Nil).tail ::: // other resources generated by wrapping the test script
            fun.resources.getOrElse(Nil).tail ::: // other resources provided in fun.resources
            tests.filter(!_.isInstanceOf[Script]) // other resources provided in fun.tests
        ))

        // make a new directory
        val newDir = dir.resolve( "test_" + test.filename)
        Files.createDirectories(newDir)

        // write resources to dir
        IO.writeResources(funFinal.resources.getOrElse(Nil), newDir)

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
          logger(s"+$executable")
          val exitValue = Process(Seq(executable), cwd = newDir.toFile).!(ProcessLogger(logger, logger))

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
