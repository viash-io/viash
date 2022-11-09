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

import java.nio.file.Paths

import io.viash.config._
import io.viash.functionality.arguments.{FileArgument, Output}
import io.viash.platforms.Platform
import io.viash.helpers.IO
import io.viash.helpers.data_structures._

import scala.sys.process.{Process, ProcessLogger}

object ViashRun {
  def apply(
    config: Config,
    platform: Platform,
    args: Seq[String],
    keepFiles: Option[Boolean],
    cpus: Option[Int],
    memory: Option[String]
  ): Int = {
    val fun = platform.modifyFunctionality(config, testing = false)
    val dir = IO.makeTemp("viash_" + fun.name)

    // execute command, print everything to console
    var code = -1
    try {
      // write executable and resources to temporary directory
      IO.writeResources(fun.resources, dir)

      // determine command
      val cmd =
        Array(Paths.get(dir.toString, fun.name).toString) ++ 
        args ++ 
        Array(cpus.map("---cpus=" + _), memory.map("---memory="+_)).flatMap(a => a)

      // execute command, print everything to console
      code = Process(cmd).!(ProcessLogger(println, println))
      // System.exit(code)
      code 
    } finally {
      // remove tempdir if desired
      if (!keepFiles.getOrElse(code != 0)) {
        IO.deleteRecursively(dir)
      } else {
        println(s"Files and logs are stored at '$dir'")
      }
    }
  }
}
