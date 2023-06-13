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

package io.viash.config

import java.nio.file.Path
import java.nio.file.Paths
import io.circe.yaml.Printer

import io.viash.functionality.resources.PlainFile
import io.viash.helpers.circe._
import io.circe.Json
import io.circe.JsonObject
import io.viash.functionality.resources.NextflowScript

object ConfigMeta {
  // create a yaml printer for writing the viash.yaml file
  // Options: https://github.com/circe/circe-yaml/blob/master/src/main/scala/io/circe/yaml/Printer.scala
  private val printer = Printer(
    preserveOrder = true,
    mappingStyle = Printer.FlowStyle.Block,
    splitLines = true,
    stringStyle = Printer.StringStyle.DoubleQuoted
  )

  val metaFilename: String = ".config.vsh.yaml"

  def configToCleanJson(config: Config): Json = {
    val encodedConfig: Json = encodeConfig(config)
    // drop empty & null values recursively except all "info" fields
    val cleanEncodedConfig = encodedConfig.dropEmptyRecursivelyExcept(Seq("info", ".platforms.entrypoint", ".platforms.cmd"))
    // get config.info and *do* clean it
    cleanEncodedConfig.mapObject(_.map{
      case ("info", v) => ("info", v.dropEmptyRecursively)
      case other => other
    })
  }

  def toMetaFile(config: Config, buildDir: Option[Path]): PlainFile = {
    // get resources
    val placeholderMap = config.functionality.resources.filter(_.text.isDefined).map{ res =>
      (res, "VIASH_PLACEHOLDER~" + res.filename + "~")
    }.toMap

    val executableName = config.functionality.mainScript match {
      case Some(n: NextflowScript) => "main.nf"
      case _ => config.functionality.name
    }

    // change the config object before writing to yaml:
    // * substitute 'text' fields in resources with placeholders
    // * add more info variables
    val toWriteConfig = config.copy(
      functionality = config.functionality.copy(
        resources = config.functionality.resources.map{ res =>
          if (res.text.isDefined) {
            val textVal = Some(placeholderMap(res))
            res.copyResource(text = textVal, parent = None)
          } else {
            res.copyResource(parent = None)
          }
        },
        test_resources = config.functionality.test_resources.map { res =>
          res.copyResource(parent = None)
        }
      ),
      info = config.info.map(_.copy(
        output = buildDir.map(_.toString),
        executable = buildDir.map(d => Paths.get(d.toString, executableName).toString)
      ))
    )

    // convert config to yaml
    val configYamlStr = printer.pretty(configToCleanJson(toWriteConfig))

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
    PlainFile(
      dest = Some(metaFilename),
      text = Some(configYamlStr2)
    )
  }
}
