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

import java.nio.file.{Paths, Files, StandardOpenOption}
import io.viash.ViashTest.{ManyTestOutput, TestOutput}
import config.Config
import helpers.{IO, Logging}
import io.viash.exceptions.MissingResourceFileException
import io.viash.helpers.status._
import java.nio.file.Path
import io.viash.helpers.NsExecData._
import io.viash.helpers.NsExecData
import sys.process._
import java.io.{ByteArrayOutputStream, File, PrintWriter}
import scala.collection.parallel.CollectionConverters._
import io.viash.helpers.LoggerOutput
import io.viash.helpers.LoggerLevel
import io.viash.runners.Runner
import io.viash.config.AppliedConfig

object ViashNamespace extends Logging {

  case class MaybeParList[T](
    list: List[T],
    parallel: Boolean
  ) {
    def map[B](f: T => B): List[B] = 
      if (parallel)
        list.par.map(f).toList
      else
        list.map(f)
    def foreach[U](f: T => U): Unit = 
      if (parallel)
        list.par.foreach(f)
      else
        list.foreach(f)
  }

  def targetOutputPath(
    targetDir: String,
    runnerId: String,
    namespace: Option[String],
    functionalityName: String
  ): String = {
    val nsStr = namespace match {
      case Some(ns) => ns + "/"
      case None => ""
    }
    s"$targetDir/$runnerId/$nsStr$functionalityName"
  }

  def build(
    configs: List[AppliedConfig],
    target: String,
    setup: Option[String] = None,
    push: Boolean = false,
    parallel: Boolean = false,
    flatten: Boolean = false
  ): List[AppliedConfig] = {
    val configs2 = MaybeParList(configs, parallel)

    val results = configs2.map { config =>
      config match {
        case conf if conf.status.isDefined => config
        case ac if !ac.validForBuild => throw new RuntimeException("This should not occur.")
        case ac =>
          val funName = ac.config.name
          val ns = ac.config.namespace
          val runnerId = ac.runner.get.id
          // val engineId = ac.platform.get.id
          val out = 
            if (flatten) {
              target
            } else {
              targetOutputPath(target, runnerId, ns, funName)
            }
          val nsStr = ns.map(" (" + _ + ")").getOrElse("")
          infoOut(s"Exporting $funName$nsStr =$runnerId=> $out")
          val status = ViashBuild(
            appliedConfig = ac,
            output = out,
            setup = setup,
            push = push
          )
          ac.setStatus(status)
        }
      }

    printResults(results.map(ac => ac.status.getOrElse(Success)).toList, true, false)
    results.toList
  }

  def test(
    configs: List[AppliedConfig],
    parallel: Boolean = false,
    setup: Option[String] = None,
    keepFiles: Option[Boolean] = None,
    tsv: Option[String] = None,
    append: Boolean = false,
    cpus: Option[Int],
    memory: Option[String]
  ): List[(AppliedConfig, ManyTestOutput)] = {
    val configs1 = configs.filter{tup => tup match {
      // remove nextflow because unit testing nextflow modules
      // is not yet supported
      // TODO: Soon they might be!
      case AppliedConfig(_, Some(ex), _, None) => ex.`type` != "nextflow"
      case _ => true
    }}
    val configs2 = MaybeParList(configs1, parallel)

    // run all the component tests
    val tsvPath = tsv.map(Paths.get(_))

    // remove if not append
    for (tsv <- tsvPath if !append) {
      Files.deleteIfExists(tsv)
    }

    val tsvExists = tsvPath.exists(Files.exists(_))
    val tsvWriter = tsvPath.map(Files.newBufferedWriter(_, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.APPEND))

    val parentTempPath = IO.makeTemp("viash_ns_test")
    if (keepFiles.getOrElse(true)) {
      info(s"The working directory for the namespace tests is ${parentTempPath.toString()}")
    }
    
    try {
      // only print header if file does not exist
      for (writer <- tsvWriter if !tsvExists) {
        writer.write(
          List(
            "namespace",
            "functionality",
            "runner",
            "engine",
            "test_name",
            "exit_code",
            "duration",
            "result"
          ).mkString("\t") + sys.props("line.separator"))
        writer.flush()
      }
      infoOut("%20s %20s %20s %20s %20s %9s %8s %20s".
        format(
          "namespace",
          "functionality",
          "runner",
          "engine",
          "test_name",
          "exit_code",
          "duration",
          "result"
        ))

      val results = configs2.map { x =>
        x match {
          case ac if ac.status.isDefined => (ac, ManyTestOutput(None, List()))
          case ac if !ac.validForBuild => throw new RuntimeException("This should not occur.")
          case ac =>
            // get attributes
            val namespace = ac.config.namespace.getOrElse("")
            val funName = ac.config.name
            val runnerName = ac.runner.get.id
            val engineName = ac.engines.head.id

            // print start message
            infoOut("%20s %20s %20s %20s %20s %9s %8s %20s".format(namespace, funName, runnerName, engineName, "start", "", "", ""))

            // run tests
            // TODO: it would actually be great if this component could subscribe to testresults messages

            val ManyTestOutput(setupRes, testRes) = try {
              ViashTest(
                appliedConfig = ac,
                setupStrategy = setup,
                keepFiles = keepFiles,
                quiet = true,
                parentTempPath = Some(parentTempPath),
                cpus = cpus,
                memory = memory
              )
            } catch {
              case e: MissingResourceFileException => 
                warn(s"viash ns: ${e.getMessage}")
                ManyTestOutput(None, List())
            }

            val testResults =
              if (setupRes.isDefined && setupRes.get.exitValue > 0) {
                Nil
              } else if (testRes.isEmpty) {
                List(TestOutput("tests", -1, "no tests found", "", 0L))
              } else {
                testRes
              }

            // print messages
            val results = setupRes.toList ::: testResults
            for (test <- results) {
              val (col, msg) = {
                if (test.exitValue > 0) {
                  (Console.RED, "ERROR")
                } else if (test.exitValue < 0) {
                  (Console.YELLOW, "MISSING")
                } else {
                  (Console.GREEN, "SUCCESS")
                }
              }

              // print message
              log(LoggerOutput.StdOut, LoggerLevel.Info, col, "%20s %20s %20s %20s %20s %9s %8s %20s".format(namespace, funName, runnerName, engineName, test.name, test.exitValue, test.duration, msg))

              if (test.exitValue != 0) {
                info(test.output)
                info(ViashTest.consoleLine)
              }

              // write to tsv
              tsvWriter.foreach{writer =>
                writer.append(List(namespace, funName, runnerName, engineName, test.name, test.exitValue, test.duration, msg).mkString("\t") + sys.props("line.separator"))
                writer.flush()
              }
            }

            // return output
            (ac, ManyTestOutput(setupRes, testRes))
          }

      }.toList

      val testResults = results.flatMap{ case (ac, ManyTestOutput(setup, tests)) =>
        val acStatus = ac.status.toList
        val testStatus =
          if (setup.isDefined && setup.get.exitValue > 0) {
            List.empty[Status]
          } else if (tests.isEmpty) {
            List(TestMissing)
          } else {
            tests.map(to => if (to.exitValue == 0) Success else TestError)
          }
        val setupStatus: List[Status] = setup.toList.map(to => if (to.exitValue == 0) Success else BuildError)
        acStatus ::: setupStatus ::: testStatus
        }
      printResults(testResults, true, true)

      results
    } catch {
      case e: Exception => 
        infoOut(e.getMessage())
        Nil
    } finally {
      tsvWriter.foreach(_.close())

      // Delete temp path if empty, otherwise fail quietly and keep.
      // (tests should have cleaned themselves according to the overall 'keep' value)
      if (!keepFiles.getOrElse(false)) {
        parentTempPath.toFile().delete()
      }

    }
  }

  def list(
    configs: List[AppliedConfig], 
    format: String = "yaml"
  ): Unit = {
    val configs2 = configs.filter(_.status.isEmpty).map(_.config)

    ViashConfig.viewMany(configs2, format)

    printResults(configs.map(ac => ac.status.getOrElse(Success)), false, false)
  }

  def exec(
    configs: List[AppliedConfig],
    command: String, 
    dryrun: Boolean, 
    parallel: Boolean
  ): Unit = {
    val configData = configs.filter(_.status.isEmpty).flatMap{ ac =>
      // TODO: Should we iterate over the engines here?
      val engineOptionList = ac.engines match {
        case Nil => List(None)
        case list => list.map(Some(_))
      }
      engineOptionList.map(eo => {
        NsExecData(ac.config.build_info.get.config, ac.config, ac.runner, eo)
      })
    }

    // check whether is empty
    if (configData.isEmpty) {
      info("No config files found to work with.")
      return
    }

    // try to match to something like "cat {arg1} foo {arg2} ;"
    // Slashes for ';' or '+' are not needed here, but let's allow it anyway
    val matchChecker = """([^{}]*\{[\w-]*\})*[^{}]*(\\?[;+])?$"""
    if (!command.matches(matchChecker)) {
      info("Invalid command syntax.")
      return
    }

    // Get all fields and trim off curly brackets
    val fields = """\{[^\{\}]*\}""".r.findAllIn(command).map(_.replaceAll("^.|.$", "")).toList
    val unfoundFields = fields.filter(configData.head.getField(_).isEmpty)
    if (!unfoundFields.isEmpty) {
      info(s"Not all substitution fields are supported fields: ${unfoundFields.mkString(" ")}.")
      return
    }

    val collectedData = command.endsWith("+") match {
      case true =>
        List(combine(configData))
      case _ =>
        configData
    }

    for (data <- MaybeParList(collectedData, parallel)) {
      // remove trailing + or ; mode character
      val commandNoMode: Either[String, String] = Right(command.replaceFirst(""" \\?[;+]$""", ""))
      val errorOrCmd = 
        fields.foldRight(commandNoMode){ (field, command) => 
          (command, data.getField(field)) match {
            case (Right(cmd), Some(dataField)) => Right(cmd.replace(s"{$field}", dataField))
            case (Right(_), None) => Left(s"Missing field $field for config at '${data.configFullPath}'")
            case (Left(reason), _) => Left(reason)
          }
        }
      
      errorOrCmd match {
        case Left(error) => 
          info(s"+ $command")
          info(s"  Error: $error")
        case Right(cmd) if dryrun =>
          info(s"+ $cmd")
        case Right(cmd) =>
          info(s"+ $cmd")
          val (exitcode, output) = runExecCommand(cmd)
          info(s"  Exit code: $exitcode\n")
          info(s"  Output:")
          infoOut(output)
      }
    }
  }

  def runExecCommand(command: String) = {
    // run command, collect output
    val stream = new ByteArrayOutputStream
    val printwriter = new PrintWriter(stream)

    val logger = (s: String) => {
      printwriter.println(s)
    }

    // run command, collect output
    try {
      val exitValue = command.!(ProcessLogger(logger, logger))
      printwriter.flush()
      (exitValue, stream.toString)
    } catch {
      case e: Throwable =>
        info(s"  Exception: $e")
        (-1, e.getMessage())
    } finally {
      printwriter.close()
    }
  }

  def printResults(statuses: Seq[Status], performedBuild: Boolean, performedTest: Boolean): Unit = {
    val successes = statuses.count(_ == Success)

    val successAction = (performedBuild, performedTest) match {
      case (false, false) => "parsed"
      case (true, false) => "built"
      case (true, true) => "built and tested"
      case (false, true) => "[Unknown action!]"
    }

    val messages = List(
      (ParseError, "configs encountered parse errors"),
      (Disabled, "configs were disabled"),
      (DependencyError, "dependency resolutions failed"),
      (MissingRunnerOrEngine, "configs could not apply an runner or engine"),
      (BuildError, "configs built failed"),
      (SetupError, "setups failed"),
      (PushError, "pushes failed"),
      (TestError, "tests failed"),
      (TestMissing, "tests missing"),
      (Success, s"configs $successAction successfully"))

    if (successes != statuses.length) {
      val disabledStatusesCount = statuses.count(s => s == Disabled || s == DisabledByQuery)
      val nonDisabledStatuses = statuses.filter(s => s != Disabled && s != DisabledByQuery)
      val indentSize = nonDisabledStatuses.length.toString().size

      warn(s"Not all configs $successAction successfully")
      if (disabledStatusesCount > 0)
        warn(s"  $disabledStatusesCount configs were disabled")
      
      for ((status, message) <- messages) {
        val count = nonDisabledStatuses.count(_ == status)
        if (count > 0)
          log(LoggerOutput.StdErr, LoggerLevel.Info, status.color, s"  ${String.format(s"%${indentSize}s",count)}/${nonDisabledStatuses.length} ${message}")
      }
    }
    else {
      log(LoggerOutput.StdErr, LoggerLevel.Info, Console.GREEN, s"All ${successes} configs $successAction successfully")
    }
  }
}
