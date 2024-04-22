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
import io.viash.platforms.Platform
import scala.collection.parallel.CollectionConverters._
import io.viash.helpers.LoggerOutput
import io.viash.helpers.LoggerLevel

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
    platformId: String,
    namespace: Option[String],
    functionalityName: String
  ): String = {
    val nsStr = namespace match {
      case Some(ns) => ns + "/"
      case None => ""
    }
    s"$targetDir/$platformId/$nsStr$functionalityName"
  }

  def build(
    configs: List[Either[(Config, Option[Platform]), Status]],
    target: String,
    setup: Option[String] = None,
    push: Boolean = false,
    parallel: Boolean = false,
    flatten: Boolean = false
  ): List[Either[(Config, Option[Platform]), Status]] = {
    val configs2 = MaybeParList(configs, parallel)

    val results = configs2.map { config =>
      config match {
        case Right(_) => config
        case Left((conf, None)) => throw new RuntimeException("This should not occur.")
        case Left((conf, Some(platform))) =>
          val funName = conf.functionality.name
          val ns = conf.functionality.namespace
          val platformId = platform.id
          val out = 
            if (flatten) {
              target
            } else {
              targetOutputPath(target, platformId, ns, funName)
            }
          val nsStr = ns.map(" (" + _ + ")").getOrElse("")
          infoOut(s"Exporting $funName$nsStr =$platformId=> $out")
          val status = ViashBuild(
            config = conf,
            platform = platform,
            output = out,
            setup = setup,
            push = push
          )
          Right(status)
        }
      }

    printResults(results.map(r => r.fold(fa => Success, fb => fb)).toList, true, false)
    results.toList
  }

  def test(
    configs: List[Either[(Config, Option[Platform]), Status]],
    parallel: Boolean = false,
    setup: Option[String] = None,
    keepFiles: Option[Boolean] = None,
    tsv: Option[String] = None,
    append: Boolean = false,
    cpus: Option[Int],
    memory: Option[String],
    dryRun: Option[Boolean] = None,
    deterministicWorkingDirectory: Option[String] = None
  ): List[Either[(Config, ManyTestOutput), Status]] = {
    val configs1 = configs.filter{tup => tup match {
      // remove nextflow because unit testing nextflow modules
      // is not yet supported
      case Left((_, Some(pl))) => pl.`type` != "nextflow"
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

    val parentTempPath = IO.makeTemp(
      name = deterministicWorkingDirectory.getOrElse("viash_ns_test"),
      parentTempPath = None,
      addRandomized = deterministicWorkingDirectory.isEmpty
    )
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
            "platform",
            "test_name",
            "exit_code",
            "duration",
            "result"
          ).mkString("\t") + sys.props("line.separator"))
        writer.flush()
      }
      infoOut("%20s %20s %20s %20s %9s %8s %20s".
        format("namespace",
          "functionality",
          "platform",
          "test_name",
          "exit_code",
          "duration",
          "result"
        ))

      val results = configs2.map { x =>
        x match {
          case Right(status) => Right(status)
          case Left((conf, None)) => throw new RuntimeException("This should not occur")
          case Left((conf, Some(platform))) =>
            // get attributes
            val namespace = conf.functionality.namespace.getOrElse("")
            val funName = conf.functionality.name
            val platName = platform.id
            val directoryName = if(namespace.isEmpty) funName else namespace.replace('/', '_') + "_" + funName

            // print start message
            infoOut("%20s %20s %20s %20s %9s %8s %20s".format(namespace, funName, platName, "start", "", "", ""))

            // run tests
            // TODO: it would actually be great if this component could subscribe to testresults messages

            val ManyTestOutput(setupRes, testRes) = try {
              ViashTest(
                config = conf,
                platform = platform,
                setupStrategy = setup,
                keepFiles = keepFiles,
                quiet = true,
                parentTempPath = Some(parentTempPath),
                cpus = cpus,
                memory = memory,
                dryRun = dryRun,
                deterministicWorkingDirectory = Some(directoryName)
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
              log(LoggerOutput.StdOut, LoggerLevel.Info, col, "%20s %20s %20s %20s %9s %8s %20s".format(namespace, funName, platName, test.name, test.exitValue, test.duration, msg))

              if (test.exitValue != 0) {
                info(test.output)
                info(ViashTest.consoleLine)
              }

              // write to tsv
              tsvWriter.foreach{writer =>
                writer.append(List(namespace, funName, platName, test.name, test.exitValue, test.duration, msg).mkString("\t") + sys.props("line.separator"))
                writer.flush()
              }
            }

            // return output
            Left((conf, ManyTestOutput(setupRes, testRes)))
          }

      }.toList

      val testResults = results.flatMap(r => r.fold(fa => 
        {
          val setupRes = fa._2.setup
          val testRes = fa._2.tests
          val testStatus =
              if (setupRes.isDefined && setupRes.get.exitValue > 0) {
                Nil
              } else if (testRes.isEmpty) {
                List(TestMissing)
              } else {
                testRes.map(to => if (to.exitValue == 0) Success else TestError)
              }
          val setupStatus = setupRes.toList.map(to => if (to.exitValue == 0) Success else BuildError)
          setupStatus ::: testStatus
        },
        fb => List(fb)))
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
    configs: List[Either[(Config, Option[Platform]), Status]], 
    format: String = "yaml", 
    parseArgumentGroups: Boolean
  ): Unit = {
    val configs2 = configs.flatMap(_.left.toOption).map(_._1)
    // val configs2 = configs.flatMap(_.left.toOption).flatMap{
    //   case (config, Some(platform)) =>
    //     if (config.platforms.exists(_.id == platform.id)) {
    //       Some(config)
    //     } else {
    //       None
    //     }
    //   case (config, None) => Some(config)
    // }
    ViashConfig.viewMany(configs2, format, parseArgumentGroups)

    printResults(configs.map(_.fold(fa => Success, fb => fb)), false, false)
  }

  def exec(
    configs: List[Either[(Config, Option[Platform]), Status]],
    command: String, 
    dryrun: Boolean, 
    parallel: Boolean
  ): Unit = {
    val configData = configs.flatMap(_.left.toOption).map{
      case (conf, plat) => 
        NsExecData(conf.info.get.config, conf, plat)
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
      (BuildError, "configs built failed"),
      (SetupError, "setups failed"),
      (PushError, "pushes failed"),
      (TestError, "tests failed"),
      (TestMissing, "tests missing"),
      (Success, s"configs $successAction successfully"))

    if (successes != statuses.length) {
      val disabledStatusesCount = statuses.count(_ == Disabled)
      val nonDisabledStatuses = statuses.filter(_ != Disabled)
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
