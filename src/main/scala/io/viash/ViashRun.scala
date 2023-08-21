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
import io.viash.helpers.{IO, Logging}
import io.viash.helpers.data_structures._

import scala.sys.process.{Process, ProcessLogger}
import io.viash.executors.Executor

object ViashRun extends Logging {
  def apply(
    config: Config,
    executor: Executor,
    args: Seq[String],
    keepFiles: Option[Boolean],
    cpus: Option[Int],
    memory: Option[String]
  ): Int = {
    val resources = executor.generateExecutor(config, testing = false)
    val dir = IO.makeTemp("viash_" + config.functionality.name)

    // execute command, print everything to console
    var code = -1
    try {
      // convert config to a yaml wrapped inside a PlainFile
      val configYaml = ConfigMeta.toMetaFile(config, Some(dir))

      // write executable and resources to temporary directory
      IO.writeResources(configYaml :: resources.resources, dir)

      // determine command
      val cmd =
        Array(Paths.get(dir.toString, config.functionality.name).toString) ++ 
        args ++ 
        Array(cpus.map("---cpus=" + _), memory.map("---memory="+_)).flatMap(a => a)

      // execute command, print everything to console
      code = Process(cmd).!(ProcessLogger(s => infoOut(s), s => infoOut(s)))
      // System.exit(code)
      code 
    } finally {
      // remove tempdir if desired
      if (!keepFiles.getOrElse(code != 0)) {
        IO.deleteRecursively(dir)
      } else {
        infoOut(s"Files and logs are stored at '$dir'")
      }
    }
  }
}
