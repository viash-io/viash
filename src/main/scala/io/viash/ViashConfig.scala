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
import scala.sys.process.Process

import io.circe.syntax.EncoderOps

import io.viash.config.Config
import io.viash.helpers.{IO, Logging}
import io.viash.helpers.circe._
import io.viash.platforms.DebugPlatform
import io.viash.config.ConfigMeta
import io.viash.exceptions.ExitException
import io.viash.executors.Executor

object ViashConfig extends Logging{

  def view(config: Config, format: String, parseArgumentGroups: Boolean): Unit = {
    val conf0 = 
      if (parseArgumentGroups) {
        config.copy(
          functionality = config.functionality.copy(
            arguments = Nil,
            argument_groups = config.functionality.allArgumentGroups,
          )
        )
      } else {
        config
      }
    val json = ConfigMeta.configToCleanJson(conf0)
    infoOut(json.toFormattedString(format))
  }

  def viewMany(configs: List[Config], format: String, parseArgumentGroups: Boolean): Unit = {
    val confs0 = configs.map{ config => 
      if (parseArgumentGroups) {
        config.copy(
          functionality = config.functionality.copy(
            arguments = Nil,
            argument_groups = config.functionality.allArgumentGroups,
          )
        )
      } else {
        config
      }
    }
    val jsons = confs0.map(c => ConfigMeta.configToCleanJson(c))
    infoOut(jsons.asJson.toFormattedString(format))
  }

  def inject(config: Config): Unit = {
    val fun = config.functionality

    // check if config has a main script
    if (fun.mainScript.isEmpty) {
      infoOut("Could not find a main script in the Viash config.")
      throw new ExitException(1)
    }
    // check if we can read code
    if (fun.mainScript.get.read.isEmpty) {
      infoOut("Could not read main script in the Viash config.")
      throw new ExitException(1)
    }
    // check if main script has a path
    if (fun.mainScript.get.uri.isEmpty) {
      infoOut("Main script should have a path.")
      throw new ExitException(1)
    }
    val uri = fun.mainScript.get.uri.get

    // check if main script is a local file
    if (uri.getScheme != "file") {
      infoOut("Config inject only works for local Viash configs.")
      throw new ExitException(1)
    }
    val path = Paths.get(uri.getPath())

    // debugFun
    val debugPlatform = DebugPlatform(path = uri.getPath())
    val resources = debugPlatform.generateExecutor(config, testing = false)

    // create temporary directory
    val dir = IO.makeTemp("viash_inject_" + config.functionality.name)

    // build regular executable
    Files.createDirectories(dir)
    IO.writeResources(resources.resources, dir)

    // run command, collect output
    val executable = Paths.get(dir.toString, fun.name).toString
    val exitValue = Process(Seq(executable), cwd = dir.toFile).!

    // TODO: remove tempdir
  }


}
