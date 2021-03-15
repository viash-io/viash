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

import java.io.FileWriter

import com.dataintuitive.viash.ViashTest.{ManyTestOutput, TestOutput}
import config.Config

object ViashNamespace {
  def build(
    configs: List[Config],
    target: String,
    setup: Boolean = false,
    push: Boolean = false,
    parallel: Boolean = false,
    writeMeta: Boolean = true
  ) {
    val configs2 = if (parallel) configs.par else configs

    configs2.foreach { conf =>
      val in = conf.info.get.parent_path
      val inTool = in.split("/").lastOption.getOrElse("WRONG")
      val platType = conf.platform.get.id
      val out =
        conf.functionality.namespace
          .map( ns => target + s"/$platType/$ns/$inTool").getOrElse(target + s"/$platType/$inTool")
      val namespaceOrNothing = conf.functionality.namespace.map( s => "(" + s + ")").getOrElse("")
      println(s"Exporting $in $namespaceOrNothing =$platType=> $out")
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
    tsv: Option[String] = None
  ): List[(Config, ManyTestOutput)] = {
    val configs2 = if (parallel) configs.par else configs

    // run all the component tests
    val tsvWriter = tsv.map(new FileWriter(_, false))

    try {
      tsvWriter.foreach(_.append(List("namespace", "functionality", "platform", "test_name", "exit_code", "duration", "result").mkString("\t") + sys.props("line.separator")))
      printf(s"%s%20s %20s %20s %20s %9s %8s %20s%s\n", "", "namespace", "functionality", "platform", "test_name", "exit_code", "duration", "result", Console.RESET)

      configs2.map { conf =>
        // get attributes
        val namespace = conf.functionality.namespace.getOrElse("")
        val funName = conf.functionality.name
        val platName = conf.platform.get.id

        // print start message
        printf(s"%s%20s %20s %20s %20s %9s %8s %20s%s\n", "", namespace, funName, platName, "start", "", "", "", Console.RESET)

        // run tests
        // TODO: it would actually be great if this component could subscribe to testresults messages
        val ManyTestOutput(setupRes, testRes) = ViashTest(
          config = conf,
          keepFiles = keepFiles,
          quiet = true
        )

        val testResults =
          if (setupRes.exitValue > 0) {
            Nil
          } else if (testRes.isEmpty) {
            List(TestOutput("tests", -1, "no tests found", "", 0L))
          } else {
            testRes
          }

        // print messages
        val results = setupRes :: testResults
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
    } finally {
      tsvWriter.foreach(_.close())
    }
  }

  def list(
    configs: List[Config]
  ) {
    // TODO: move the functionality here to a dedicated helper
    // TODO2: align with viash config view

    import config._
    import io.circe.yaml.Printer
    import helpers.IO
    val printer = Printer(
      preserveOrder = true,
      dropNullKeys = true,
      mappingStyle = Printer.FlowStyle.Block,
      splitLines = true,
      stringStyle = Printer.StringStyle.DoubleQuoted
    )
    println(
      // configs.map(_.functionality)
      printer.pretty(encodeListConfig(configs))
    )

  }
}
