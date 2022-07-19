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

package com.dataintuitive.viash.helpers

import io.viash.config.Config
import java.nio.file.Paths

final case class NsExecData(
  configFullPath: String,
  absoluteConfigFullPath: String,
  dir: String,
  absoluteDir: String,
  mainScript: String,
  absoluteMainScript: String,
  functionalityName: String
)

object NsExecData {
  def apply(configPath: String, config: Config):NsExecData = {
    val configPath_ = Paths.get(configPath)
    val mainScript = config.functionality.mainScript.flatMap(s => s.path)
    val absoluteMainScript = mainScript.flatMap(m => Some(Paths.get(m).toAbsolutePath.toString))
    NsExecData(
      configFullPath = configPath,
      absoluteConfigFullPath = configPath_.toAbsolutePath.toString,
      dir = configPath_.getParent.toString,
      absoluteDir = configPath_.toAbsolutePath.getParent.toString,
      mainScript = mainScript.getOrElse(""),
      absoluteMainScript = absoluteMainScript.getOrElse(""),
      functionalityName = config.functionality.name
    )
  }

  def getField(data: NsExecData, name: String) = {
    name match {
      case "" | "path" => Some(data.configFullPath)
      case "abs-path" => Some(data.absoluteConfigFullPath)
      case "dir" => Some(data.dir)
      case "abs-dir" => Some(data.absoluteDir)
      case "main-script" => Some(data.mainScript)
      case "abs-main-script" => Some(data.absoluteMainScript)
      case "functionality-name" => Some(data.functionalityName)
      case _ => None
    }
  }
}
