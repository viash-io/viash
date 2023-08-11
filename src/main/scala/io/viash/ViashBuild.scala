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

import java.nio.file.{Files, Paths}
import scala.sys.process.{Process, ProcessLogger}
import io.viash.helpers.status._

import config._
import platforms.Platform
import helpers.{IO, Logging}

object ViashBuild extends Logging {
  def apply(
    config: Config,
    platform: Platform,
    output: String,
    setup: Option[String] = None,
    push: Boolean = false
  ): Status = {
    val fun = platform.modifyFunctionality(config, testing = false)

    // create dir
    val dir = Paths.get(output)
    Files.createDirectories(dir)

    // get the path of where the executable will be written to
    val exec_path = fun.mainScript.map(scr => Paths.get(output, scr.resourcePath).toString)

    // convert config to a yaml wrapped inside a PlainFile
    val configYaml = ConfigMeta.toMetaFile(config, Some(dir))

    // write resources to output directory
    IO.writeResources(configYaml :: fun.resources, dir)

    // if '--setup <strat>' was passed, run './executable ---setup <strat>'
    val setupResult =
      if (setup.isDefined && exec_path.isDefined && platform.hasSetup) {
        val cmd = Array(exec_path.get, "---setup", setup.get)
        val res = Process(cmd).!(ProcessLogger(s => infoOut(s), s => infoOut(s)))
        res
      }
      else 0

    // if '--push' was passed, run './executable ---setup push'
    val pushResult =
      if (push && exec_path.isDefined && platform.hasSetup) {
        val cmd = Array(exec_path.get, "---setup push")
        val _ = Process(cmd).!(ProcessLogger(s => infoOut(s), s => infoOut(s)))
      }
      else 0
    
    (setupResult, pushResult) match {
      case (0, 0) => Success
      case (1, _) => SetupError
      case (0, 1) => PushError
    }
  }
}
