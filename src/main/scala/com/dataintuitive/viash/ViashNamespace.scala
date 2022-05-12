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

import java.nio.file.{Paths, Files, StandardOpenOption}
import com.dataintuitive.viash.ViashTest.{ManyTestOutput, TestOutput}
import config.Config
import helpers.IO
import com.dataintuitive.viash.helpers.MissingResourceFileException

object ViashNamespace {
  def build(
    configs: List[Config],
    target: String,
    setup: Option[String] = None,
    push: Boolean = false,
    parallel: Boolean = false,
    writeMeta: Boolean = true,
    flatten: Boolean = false
  ) {
    val configs2 = if (parallel) configs.par else configs

    configs2.foreach { conf =>
      val funName = conf.functionality.name
      val platformId = conf.platform.get.id
      val out =
        if (!flatten) {
          conf.functionality.namespace
            .map( ns => target + s"/$platformId/$ns/$funName").getOrElse(target + s"/$platformId/$funName")
        } else {
          target
        }
      val namespaceOrNothing = conf.functionality.namespace.map( s => "(" + s + ")").getOrElse("")
      println(s"Exporting $funName $namespaceOrNothing =$platformId=> $out")
      ViashBuild(
        config = conf,
        output = out,
        namespace = conf.functionality.namespace,
        setup = setup,
        push = push,
        writeMeta = writeMeta
      )
    }
  }

  def test(
    configs: List[Config],
    parallel: Boolean = false,
    keepFiles: Option[Boolean] = None,
    tsv: Option[String] = None,
    append: Boolean = false
  ): List[(Config, ManyTestOutput)] = {
    val configs2 = if (parallel) configs.par else configs

    // run all the component tests
    val tsvPath = tsv.map(Paths.get(_))
    val tsvExists = tsvPath.exists(Files.exists(_))
    val tsvWriter = tsvPath.map(Files.newBufferedWriter(_, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.APPEND))

    val parentTempPath = IO.makeTemp("viash_ns_test")
    if (keepFiles.getOrElse(true)) {
      printf("The working directory for the namespace tests is %s\n", parentTempPath.toString())
    }
    
    try {
      if (!append || !tsvExists)
        tsvWriter.foreach { writer =>
          writer.append(
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
      printf(
        s"%s%20s %20s %20s %20s %9s %8s %20s%s\n",
        "",
        "namespace",
        "functionality",
        "platform",
        "test_name",
        "exit_code",
        "duration",
        "result",
        Console.RESET
      )

      configs2.map { conf =>
        // get attributes
        val namespace = conf.functionality.namespace.getOrElse("")
        val funName = conf.functionality.name
        val platName = conf.platform.get.id

        // print start message
        printf(s"%s%20s %20s %20s %20s %9s %8s %20s%s\n", "", namespace, funName, platName, "start", "", "", "", Console.RESET)

        // run tests
        // TODO: it would actually be great if this component could subscribe to testresults messages
        var setupRes: Option[TestOutput] = None
        var testRes = List[TestOutput]()
        try {
          val ManyTestOutput(setupRes_, testRes_) = ViashTest(
            config = conf,
            keepFiles = keepFiles,
            quiet = true,
            parentTempPath = Some(parentTempPath)
          )
          setupRes = setupRes_
          testRes = testRes_
        } catch {
          case e: MissingResourceFileException => 
            System.err.printf(s"%sviash ns: %s%s\n",Console.YELLOW, e.getMessage, Console.RESET)
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
        for (test ← results) {
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
          printf(s"%s%20s %20s %20s %20s %9s %8s %20s%s\n", col, namespace, funName, platName, test.name, test.exitValue, test.duration, msg, Console.RESET)

          // write to tsv
          tsvWriter.foreach{writer =>
            writer.append(List(namespace, funName, platName, test.name, test.exitValue, test.duration, msg).mkString("\t") + sys.props("line.separator"))
            writer.flush()
          }
        }

        // return output
        (conf, ManyTestOutput(setupRes, testRes))
      }.toList
    } catch {
      case e: Exception => 
        println(e.getMessage())
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

  def list(configs: List[Config], format: String = "yaml") {
    ViashConfig.viewMany(configs, format)
  }
}
