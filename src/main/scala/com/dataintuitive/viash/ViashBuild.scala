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

import config._
import functionality.resources.{BashScript, Executable, PlainFile, PythonScript, RScript}
import io.circe.yaml.Printer
import helpers.IO

import java.nio.file.{Files, Paths}
import scala.sys.process.{Process, ProcessLogger}

object ViashBuild {
  // create a yaml printer for writing the viash.yaml file
  // Options: https://github.com/circe/circe-yaml/blob/master/src/main/scala/io/circe/yaml/Printer.scala
  private val printer = Printer(
    preserveOrder = true,
    dropNullKeys = true,
    mappingStyle = Printer.FlowStyle.Block,
    splitLines = true,
    stringStyle = Printer.StringStyle.DoubleQuoted
  )

  def apply(
    config: Config,
    output: String,
    writeMeta: Boolean = false,
    printMeta: Boolean = false,
    namespace: Option[String] = None,
    setup: Option[String] = None,
    push: Boolean = false
  ) {
    val fun = config.functionality

    // create dir
    val dir = Paths.get(output)
    Files.createDirectories(dir)

    // get the path of where the executable will be written to
    val exec_path = fun.mainScript.map(scr => Paths.get(output, scr.resourcePath).toString)

    // get resources
    val placeholderMap = config.functionality.resources.filter(_.text.isDefined).map{ res =>
      (res, "VIASH_PLACEHOLDER~" + res.filename + "~")
    }.toMap

    // change the config object before writing to yaml:
    // * add more info variables
    // * remove other platforms other than the one finally used
    // * override namespace in functionality
    // * substitute 'text' fields in resources with placeholders
    val toWriteConfig = config.copy(
      functionality = config.functionality.copy(
        namespace = namespace,
        resources = config.functionality.resources.map{ res =>
          if (res.text.isDefined) {
            val textVal = Some(placeholderMap(res))
            res.copyResource(text = textVal, parent = None)
          } else {
            res.copyResource(parent = None)
          }
        },
        tests = Some(config.functionality.tests.getOrElse(Nil).map { res =>
          res.copyResource(parent = None)
        })
      ),
      info = config.info.map(_.copy(
        output = Some(output),
        executable = exec_path
      )),
      platforms = Nil // drop other platforms
    )

    // convert config to yaml
    val configYamlStr = printer.pretty(encodeConfig(toWriteConfig))

    // replace text placeholders with nice multiline string
    val configYamlStr2 = placeholderMap.foldLeft(configYamlStr) {
      case (configStr, (res, placeholder)) =>
        val IndentRegex = ("( *)text: \"" + placeholder + "\"").r
        val IndentRegex(indent) = IndentRegex.findFirstIn(configStr).getOrElse("")
        configStr.replace(
          "\"" + placeholder + "\"",
          "|\n" + indent + "  " + res.text.get.replace("\n", "\n  " + indent) + "\n"
        )
    }

    // add to resources
    val configYaml = PlainFile(
      dest = Some("viash.yaml"),
      text = Some(configYamlStr2)
    )

    // write resources to output directory
    if (writeMeta) {
      IO.writeResources(configYaml :: fun.resources, dir)
    } else {
      IO.writeResources(fun.resources, dir)
    }

    // if '--setup <strat>' was passed, run './executable ---setup <strat>'
    if (setup.isDefined && exec_path.isDefined && config.platform.exists(_.hasSetup)) {
      val cmd = Array(exec_path.get, "---setup", setup.get)
      val _ = Process(cmd).!(ProcessLogger(println, println))
    }

    // if '--push' was passed, run './executable ---setup push'
    if (push && exec_path.isDefined && config.platform.exists(_.hasSetup)) {
      val cmd = Array(exec_path.get, "---setup push")
      val _ = Process(cmd).!(ProcessLogger(println, println))
    }

    // if '-m' was passed, print some yaml about the created output fiels
    if (printMeta) {
      println(toWriteConfig.info.get.consoleString)
    }
  }
}
