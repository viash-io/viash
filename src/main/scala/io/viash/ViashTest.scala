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
import config.{Config, ArgumentGroup}
import config.arguments.{FileArgument, Output}
import config.resources.{BashScript, Script}
import helpers.{IO, Logging, LoggerOutput, LoggerLevel}
import io.viash.helpers.data_structures._
import io.viash.exceptions.MissingResourceFileException
import io.viash.config.ConfigMeta
import io.viash.helpers.DependencyResolver
import io.viash.runners.Runner
import io.viash.config.AppliedConfig
import io.viash.lenses.AppliedConfigLenses._
import io.viash.lenses.ConfigLenses.resourcesLens
import io.viash.runners.ExecutableRunner
import io.viash.engines.NativeEngine

object ViashTest extends Logging {
  case class TestOutput(name: String, exitValue: Int, output: String, logFile: String, duration: Long)
  case class ManyTestOutput(setup: Option[TestOutput], tests: List[TestOutput])

  /**
    * Run a component's unit tests
    *
    * @param appliedConfig A Viash config with runners and engines applied.
    * @param keepFiles Whether to keep temporary files after completion or remove them. 
    *   `Some(true)` means all files will be kept. `Some(false)` means they will be removed. 
    *   `None` means they will be kept if any of the unit tests errored, otherwise removed.
    * @param quiet Whether to output additional information during testing.
    * @param setupStrategy Which Docker setup strategy to use during the executable build (if applicable).
    * @param tempVersion Whether to use a random tag for the temporary Docker image.
    * @param verbosityLevel The verbosity level of the unit test.
    * @param parentTempPath A parent temporary directory.
    * @param cpus How many logical CPUs to use during testing.
    * @param memory How much memory to use during testing.
    * @return A ManyTestOutput containing the results of the unit tests.
    */
  def apply(
    appliedConfig: AppliedConfig,
    keepFiles: Option[Boolean] = None,
    quiet: Boolean = false,
    setupStrategy: Option[String] = None,
    tempVersion: Option[String] = Some("test"),
    verbosityLevel: Int = 6,
    parentTempPath: Option[Path] = None, 
    cpus: Option[Int], 
    memory: Option[String],
    dryRun: Option[Boolean] = None,
    deterministicWorkingDirectory: Option[String] = None,
  ): ManyTestOutput = {
    // create temporary directory
    val dir = IO.makeTemp(
      name = deterministicWorkingDirectory.getOrElse(s"viash_test_${configNameLens.get(appliedConfig)}_"),
      parentTempPath = parentTempPath,
      addRandomized = deterministicWorkingDirectory.isEmpty
    )
    if (!quiet) infoOut(s"Running tests in temporary directory: '$dir'")

    DependencyResolver.createBuildYaml(dir.toString())

    // set version to temporary value
    // Make dependencies available for the tests
    // Pass the first engine to the config. If no engines were specified in the config, a native engine was added.
    val modifyLenses = 
      configVersionLens.modify(version => tempVersion orElse version) andThen
      configLens.modify{ conf => DependencyResolver.copyDependencies(conf, dir.toString(), appliedConfig.runner.get.id) } andThen
      appliedEnginesLens.modify(_.take(1))
    val modifiedAppliedConfig = modifyLenses(appliedConfig)

    // run tests
    val ManyTestOutput(setupRes, results) = ViashTest.runTests(
      appliedConfig = modifiedAppliedConfig,
      dir = dir,
      verbose = !quiet,
      setupStrategy = setupStrategy.getOrElse("cachedbuild"),
      verbosityLevel = verbosityLevel,
      cpus = cpus,
      memory = memory,
      dryRun = dryRun
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
        warnOut(s"WARNING! No tests found!")
      } else if (anyErrors) {
        errorOut(s"ERROR! $errorMessage")
      } else {
        successOut(s"SUCCESS! All $count out of ${results.length} test scripts succeeded!")
      }
    }

    // keep temp files if user asks or any errors are encountered

    if (!keepFiles.getOrElse(anyErrors)) {
      if (!quiet) infoOut("Cleaning up temporary directory")
      IO.deleteRecursively(dir)
    }
    // TODO: remove container

    if (anyErrors && !quiet) {
      throw new RuntimeException(errorMessage)
    }

    ManyTestOutput(setupRes, results)
  }

  val consoleLine = "===================================================================="

  def runTests(
    appliedConfig: AppliedConfig,
    dir: Path, 
    verbose: Boolean = true, 
    setupStrategy: String, 
    verbosityLevel: Int, 
    cpus: Option[Int], 
    memory: Option[String],
    dryRun: Option[Boolean]
  ): ManyTestOutput = {
    val conf = appliedConfig.config

    assert(appliedConfig.engines.length == 1, s"Expected exactly one engine to be applied to the config. Got ${appliedConfig.engines}.")
    val engine = appliedConfig.engines.head

    // check to see if we need to set up an engine environment
    val buildResult =
      if (engine.hasSetup) {
        // use an executable to set up the engine environments
        val ac1 = appliedConfig.copy(
          runner = Some(ExecutableRunner())
        )

        // generate runner for engine environments
        val resources = ac1.generateRunner(true)
        val buildDir = dir.resolve("build_engine_environment")
        Files.createDirectories(buildDir)
        try {
          IO.writeResources(resources.resources ::: conf.test_resources.filter(!_.isInstanceOf[Script]), buildDir)
        } catch {
          case e: MissingResourceFileException =>
            // add config file name to the exception and throw again
            if (appliedConfig.config.build_info.isDefined && e.config == "") {
              throw MissingResourceFileException(e.resource, Some(appliedConfig.config.build_info.get.config), cause = e.cause)
            }
            throw e
        }

        // run engine setup commands, collect output
        // todo: setupStrategy might need to be handled differently when non-docker engines need setting up.
        val stream = new ByteArrayOutputStream
        val printWriter = new PrintWriter(stream)
        val logPath = Paths.get(buildDir.toString, "_viash_build_log.txt").toString
        val logWriter = new FileWriter(logPath, true)

        val logger: String => Unit =
          (s: String) => {
            if (verbose) infoOut(s)
            printWriter.println(s)
            logWriter.append(s + sys.props("line.separator"))
          }

        logger(consoleLine)

        // run command, collect output
      
        try {
          val executable = Paths.get(buildDir.toString, conf.name).toString
          val cmd = Seq(executable, "---verbosity", verbosityLevel.toString, "---setup", setupStrategy, "---engine", engine.id)
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
      } else {
        None
      }

    // if setup failed, return faster
    if (buildResult.exists(_.exitValue > 0)) {
      return ManyTestOutput(buildResult, Nil)
    }

    // generate executable runner
    val exeConfig = appliedConfig.config.copy(
      engines = List(NativeEngine())
    )
    val exe = ExecutableRunner().generateRunner(exeConfig, true).resources.head

    // fetch tests
    val tests = conf.test_resources

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
          direction = Output, // why output?
          default = OneOrMore(dir),
          must_exist = false,
          create_parent = false
        )

        val testFunConfig = configLens.modify(
          conf => Config(
            // set same name, namespace and version
            // to be able to reuse same docker container
            name = conf.name,
            namespace = conf.namespace,
            version = conf.version,
            // set dirArg as argument so that Docker can chown it after execution
            argument_groups = List(ArgumentGroup("default", arguments = List(dirArg))),
            resources = List(test),
            set_wd_to_resources_dir = true,
            // Make sure we'll be using the same docker registry set in 'links' so we can have the same docker image id.
            // Copy the whole case class instead of selective copy.
            links = conf.links,
          ))(appliedConfig)

        // generate bash script for test
        val resourcesOnlyTest = testFunConfig.generateRunner(true)
        val testBash = BashScript(
          dest = Some("test_executable"),
          text = resourcesOnlyTest.resources.head.text
        )
        val configYaml = ConfigMeta.toMetaFile(appliedConfig, Some(dir))

        // assemble full resources list for test
        val confFinal = resourcesLens.set(
          testBash ::
          // the executable, wrapped with an executable runner,
          // to be run inside of the runner of the test
          exe :: 
          // the config file information
          configYaml ::
          // other resources generated by wrapping the test script
          resourcesOnlyTest.additionalResources :::
          // other resources provided in fun.resources
          conf.additionalResources :::
          // other resources provided in fun.tests
          tests.filter(!_.isInstanceOf[Script])
        )(conf)

        // write resources to dir
        IO.writeResources(confFinal.resources, newDir)

        // run command, collect output
        val stream = new ByteArrayOutputStream
        val printWriter = new PrintWriter(stream)
        val logPath = Paths.get(newDir.toString, "_viash_test_log.txt").toString
        val logWriter = new FileWriter(logPath, true)

        val logger: String => Unit =
          (s: String) => {
            if (verbose) infoOut(s)
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

          val cmd = if (dryRun.getOrElse(false))
            Seq("echo", "Running dummy test script")
          else
            Seq(executable) ++ Seq(cpus.map("---cpus=" + _), memory.map("---memory="+_)).flatMap(a => a)
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

    if (verbose) infoOut(consoleLine)

    ManyTestOutput(buildResult, testResults)
  }

}
