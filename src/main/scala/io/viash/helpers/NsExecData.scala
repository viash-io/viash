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

final case class NsExecData(
  configFullPath: String,
  absoluteConfigFullPath: String,
  dir: String,
  absoluteDir: String,
  mainScript: String,
  absoluteMainScript: String,
  functionalityName: String,
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
    val configPath_ = Paths.get(configPath)
    val dirPath = configPath_.getParent()
    val mainScript = config.functionality.mainScript.flatMap(s => s.path).map(dirPath.resolve(_))
    apply(
      configFullPath = configPath,
      absoluteConfigFullPath = configPath_.toAbsolutePath.toString,
      dir = dirPath.toString,
      absoluteDir = dirPath.toAbsolutePath.toString,
      mainScript = mainScript.map(_.toString).getOrElse(""),
      absoluteMainScript = mainScript.map(_.toAbsolutePath.toString).getOrElse(""),
      functionalityName = config.functionality.name,
      namespace = config.functionality.namespace,
      runnerId = runner.map(_.id),
      engineId = engine.map(_.id),
      output = config.info.flatMap(_.output),
      absoluteOutput = config.info.flatMap(info => info.output.map(Paths.get(_).toAbsolutePath.toString))
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
      namespace = Some(data.flatMap(_.namespace).mkString(" ")),
      runnerId = Some(data.flatMap(_.runnerId).mkString(" ")),
      engineId = Some(data.flatMap(_.engineId).mkString(" ")),
      output = Some(data.flatMap(_.output).mkString(" ")),
      absoluteOutput = Some(data.flatMap(_.absoluteOutput).mkString(" "))
    )
  }
}
