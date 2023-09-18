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
import helpers.{IO, Logging}
import io.viash.runners.Runner

object ViashBuild extends Logging {
  def apply(
    appliedConfig: AppliedConfig,
    output: String,
    setup: Option[String] = None,
    push: Boolean = false
  ): Status = {
    val resources = appliedConfig.generateRunner(false)

    // create dir
    val dir = Paths.get(output)
    Files.createDirectories(dir)

    // get the path of where the executable will be written to
    val exec_path = resources.mainScript.map(scr => Paths.get(output, scr.resourcePath).toString)

    // convert config to a yaml wrapped inside a PlainFile
    val configYaml = ConfigMeta.toMetaFile(appliedConfig.config, Some(dir))

    // write resources to output directory
    IO.writeResources(configYaml :: resources.resources, dir)

    // todo: should setup be deprecated?
    // todo: should push be deprecated?
    if (setup.isEmpty || exec_path.isEmpty) {
      return Success
    }

    // if '--setup <strat>' was passed, run './executable ---setup <strat> ---engine <engine>'
    if (setup.isDefined && exec_path.isDefined) {
      val exitCodes = appliedConfig.engines.map{ engine => 
        if (engine.hasSetup) {
          val cmd = Array(exec_path.get, "---setup", setup.get, "---engine", engine.id)
          val res = Process(cmd).!(ProcessLogger(s => infoOut(s), s => infoOut(s)))
          res
        } else {
          0
        }
      }
      if (exitCodes.exists(_ != 0)) {
        return SetupError
      }
    }

    if (push && exec_path.isDefined) {
      val exitCodes = appliedConfig.engines.map{ engine => 
        if (engine.hasSetup) {
          val cmd = Array(exec_path.get, "---setup", "push", "---engine", engine.id)
          val res = Process(cmd).!(ProcessLogger(s => infoOut(s), s => infoOut(s)))
          res
        } else {
          0
        }
      }
      if (exitCodes.exists(_ != 0)) {
        return PushError
      }
    }

    return Success
  }
}
