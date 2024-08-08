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

import io.viash.config.resources.PlainFile
import io.viash.helpers.circe._
import io.circe.Json
import io.circe.JsonObject
import io.viash.config.resources.NextflowScript
import io.viash.helpers.IO
import io.viash.runners.NextflowRunner

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
    // relativize paths in the info field
    val rootDir = config.package_config.flatMap(_.rootDir)

    // get a list of all dependency paths in an anonymized way, None if there are no dependencies
    val dependencyPaths = config.dependencies.map(_.writtenPath).flatMap(_.map(IO.anonymizePath(rootDir, _)))
    val dependencies = Some(dependencyPaths).filter(_.nonEmpty)

    val anonymizedConfig = config.copy(
      build_info = config.build_info.map(info => info.copy(
        config = IO.anonymizePath(rootDir, info.config),
        output = info.output.map(IO.anonymizePath(rootDir, _)),
        executable = info.executable.map(IO.anonymizePath(rootDir, _)),
        dependencies = dependencies
      )),
      package_config = config.package_config.map(pc => pc.copy(
        source = pc.source.map(IO.anonymizePath(rootDir, _)),
        target = pc.target.map(IO.anonymizePath(rootDir, _))
      ))
    )

    val encodedConfig: Json = Json.Null//encodeConfig(anonymizedConfig)
    // drop empty & null values recursively except all "info" fields
    val cleanEncodedConfig = encodedConfig.dropEmptyRecursivelyExcept(Seq("info", ".engines.entrypoint", ".engines.cmd"))
    // get config.info and *do* clean it
    cleanEncodedConfig.mapObject(_.map{
      case ("info", v) => ("info", v.dropEmptyRecursively)
      case other => other
    })
  }

  def toMetaFile(appliedConfig: AppliedConfig, buildDir: Option[Path]): PlainFile = {
    val config = appliedConfig.config

    // get resources
    val placeholderMap = config.resources.filter(_.text.isDefined).map{ res =>
      (res, "VIASH_PLACEHOLDER~" + res.filename + "~")
    }.toMap

    val executableName = appliedConfig.runner match {
      case Some(_: NextflowRunner) => "main.nf"
      case _ => config.name
    }

    // change the config object before writing to yaml:
    // * substitute 'text' fields in resources with placeholders
    // * set 'path' fields to the resourcePath
    // * add more info variables
    val toWriteConfig = config.copy(
      resources = config.resources.map{ res =>
        if (res.text.isDefined) {
          val textVal = Some(placeholderMap(res))
          res.copyResource(text = textVal, parent = None)
        } else {
          res.copyResource(parent = None, path = Some(res.resourcePath))
        }
      },
      test_resources = config.test_resources.map { res =>
        if (res.text.isDefined) {
          res.copyResource(parent = None)
        } else {
          res.copyResource(parent = None, path = Some(res.resourcePath))
        }
      },
      build_info = config.build_info.map(_.copy(
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
