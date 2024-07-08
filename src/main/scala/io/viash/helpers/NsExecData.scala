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

package io.viash.helpers

import io.viash.config.Config
import java.nio.file.Paths
import io.viash.schemas.since
import io.viash.runners.Runner
import io.viash.engines.Engine
import io.viash.config_mods.ConfigModParser.root

final case class NsExecData(
  configFullPath: String,
  absoluteConfigFullPath: String,
  dir: String,
  absoluteDir: String,
  mainScript: String,
  absoluteMainScript: String,
  functionalityName: String,
  name: String,
  namespace: Option[String],
  runnerId: Option[String],
  engineId: Option[String],

  @since("Viash 0.7.4")
  output: Option[String],
  
  @since("Viash 0.7.4")
  absoluteOutput: Option[String]
) {
  def getField(name: String) = {
    name match {
      case "" | "path" => Some(this.configFullPath)
      case "abs-path" => Some(this.absoluteConfigFullPath)
      case "dir" => Some(this.dir)
      case "abs-dir" => Some(this.absoluteDir)
      case "main-script" => Some(this.mainScript)
      case "abs-main-script" => Some(this.absoluteMainScript)
      case "functionality-name" => Some(this.functionalityName)
      case "name" => Some(this.functionalityName)
      case "namespace" => this.namespace
      case "runner" => this.runnerId
      case "engine" => this.engineId
      case "output" => this.output
      case "abs-output" => this.absoluteOutput
      case _ => None
    }
  }
}

object NsExecData {
  def apply(configPath: String, config: Config, runner: Option[Runner], engine: Option[Engine]): NsExecData = {
    val parentPath = config.package_config.flatMap(_.rootDir)
    val configPathRel = parentPath match {
      case Some(rootDir) => rootDir.relativize(Paths.get(configPath))
      case None => Paths.get(configPath)
    }
    val configPathAbs = Paths.get(configPath)
    val dirPathRel = configPathRel.getParent()
    val dirPathAbs = configPathAbs.getParent().toAbsolutePath()
    val mainScript = config.mainScript.flatMap(_.resolvedPath)
    val mainScriptRel = mainScript.map(dirPathRel.resolve(_))
    val mainScriptAbs = mainScript.map(dirPathAbs.resolve(_).toAbsolutePath())
    val outputDir = config.build_info.flatMap(_.output).map(Paths.get(_))
    val outputRel = (outputDir, parentPath) match {
      case (Some(outputDir), Some(rootDir)) => Some(rootDir.relativize(outputDir))
      case (Some(outputDir), None) => Some(outputDir)
      case _ => None
    }
    val outputAbs = outputDir.map(_.toAbsolutePath)
    apply(
      configFullPath = configPathRel.toString(),
      absoluteConfigFullPath = configPathAbs.toAbsolutePath.toString,
      dir = dirPathRel.toString,
      absoluteDir = dirPathAbs.toString,
      mainScript = mainScriptRel.map(_.toString).getOrElse(""),
      absoluteMainScript = mainScriptAbs.map(_.toString).getOrElse(""),
      functionalityName = config.name,
      name = config.name,
      namespace = config.namespace,
      runnerId = runner.map(_.id),
      engineId = engine.map(_.id),
      output = outputRel.map(_.toString()),
      absoluteOutput = outputAbs.map(_.toString())
    )
  }

  def combine(data: Iterable[NsExecData]) = {
    apply(
      configFullPath = data.map(_.configFullPath).mkString(" "),
      absoluteConfigFullPath = data.map(_.absoluteConfigFullPath).mkString(" "),
      dir = data.map(_.dir).mkString(" "),
      absoluteDir = data.map(_.absoluteDir).mkString(" "),
      mainScript = data.map(_.mainScript).mkString(" "),
      absoluteMainScript = data.map(_.absoluteMainScript).mkString(" "),
      functionalityName = data.map(_.functionalityName).mkString(" "),
      name = data.map(_.name).mkString(" "),
      namespace = Some(data.flatMap(_.namespace).mkString(" ")),
      runnerId = Some(data.flatMap(_.runnerId).mkString(" ")),
      engineId = Some(data.flatMap(_.engineId).mkString(" ")),
      output = Some(data.flatMap(_.output).mkString(" ")),
      absoluteOutput = Some(data.flatMap(_.absoluteOutput).mkString(" "))
    )
  }
}
