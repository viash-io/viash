package com.dataintuitive.viash

import functionality._
import dataobjects.{FileObject, Output}
import platforms._
import resources.{BashScript, Script}

import sys.process.{Process, ProcessLogger}
import java.io.{ByteArrayOutputStream, File, FileWriter, PrintWriter}
import java.nio.file.Paths

import com.dataintuitive.viash.config.Config
import helpers.IO

import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

object ViashTest {
  case class TestOutput(name: String, exitValue: Int, output: String, logFile: String, duration: Long)
  case class ManyTestOutput(setup: TestOutput, tests: List[TestOutput])

  def apply(
    config: Config,
    keepFiles: Option[Boolean] = None,
    quiet: Boolean = false
  ): ManyTestOutput = {
    // create temporary directory
    val dir = IO.makeTemp("viash_test_" + config.functionality.name)
    if (!quiet) println(s"Running tests in temporary directory: '$dir'")

    // run tests
    val ManyTestOutput(setupRes, results) = ViashTest.runTests(config, dir, verbose = !quiet)
    val count = results.count(_.exitValue == 0)
    val anyErrors = setupRes.exitValue > 0 || count < results.length

    val errorMessage =
      if (!anyErrors) {
        ""
      } else if (setupRes.exitValue > 0) {
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

  def runTests(config: Config, dir: File, verbose: Boolean = true): ManyTestOutput = {
    val fun = config.functionality
    val platform = config.platform.get

    val consoleLine = "===================================================================="

    // build regular executable
    val buildFun = platform.modifyFunctionality(fun)
    val buildDir = Paths.get(dir.toString, "build_executable").toFile
    buildDir.mkdir()
    IO.writeResources(buildFun.resources.getOrElse(Nil), buildDir)

    // run command, collect output
    val buildResult = {
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
        logger(s"+$executable ---setup")
        val startTime = LocalDateTime.now
        val exitValue = Process(Seq(executable, "---setup"), cwd = buildDir).!(ProcessLogger(logger, logger))
        val endTime = LocalDateTime.now
        val diffTime = ChronoUnit.SECONDS.between(startTime, endTime)
        printWriter.flush()
        TestOutput("build_executable", exitValue, stream.toString, logPath, diffTime)
      } finally {
        printWriter.close()
        logWriter.close()
      }
    }

    // if setup failed, return faster
    if (buildResult.exitValue > 0) {
      return ManyTestOutput(buildResult, Nil)
    }

    // generate executable for native platform
    val exe = NativePlatform(version = None).modifyFunctionality(fun).resources.get.head

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
          set_wd_to_resources_dir = Some(true)))
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
        val newDir = Paths.get(dir.toString, "test_" + test.filename).toFile
        newDir.mkdir()

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

        // run command, collect output
        try {
          val executable = Paths.get(newDir.toString, testBash.filename).toString
          logger(s"+$executable")
          val exitValue = Process(Seq(executable), cwd = newDir).!(ProcessLogger(logger, logger))

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
