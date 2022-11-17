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

import config._
import platforms.Platform
import helpers.IO

object ViashBuild {
  def apply(
    config: Config,
    platform: Platform,
    output: String,
    writeMeta: Boolean = false,
    printMeta: Boolean = false,
    namespace: Option[String] = None,
    setup: Option[String] = None,
    push: Boolean = false
  ) {
    val fun = platform.modifyFunctionality(config, testing = false)

    // create dir
    val dir = Paths.get(output)
    Files.createDirectories(dir)

    // get the path of where the executable will be written to
    val exec_path = fun.mainScript.map(scr => Paths.get(output, scr.resourcePath).toString)

    // change the config object before writing to yaml:
    // * add more info variables
    val toWriteConfig = config.copy(
      info = config.info.map(_.copy(
        output = Some(output),
        executable = exec_path
      ))
    )

    // convert config to a yaml wrapped inside a PlainFile
    val configYaml = ConfigMeta.toMetaFile(toWriteConfig)

    // write resources to output directory
    if (writeMeta) {
      IO.writeResources(configYaml :: fun.resources, dir)
    } else {
      IO.writeResources(fun.resources, dir)
    }

    // if '--setup <strat>' was passed, run './executable ---setup <strat>'
    if (setup.isDefined && exec_path.isDefined && platform.hasSetup) {
      val cmd = Array(exec_path.get, "---setup", setup.get)
      val _ = Process(cmd).!(ProcessLogger(println, println))
    }

    // if '--push' was passed, run './executable ---setup push'
    if (push && exec_path.isDefined && platform.hasSetup) {
      val cmd = Array(exec_path.get, "---setup push")
      val _ = Process(cmd).!(ProcessLogger(println, println))
    }

    // if '-m' was passed, print some yaml about the created output fields
    if (printMeta) {
      println(toWriteConfig.info.get.consoleString)
    }
  }
}
