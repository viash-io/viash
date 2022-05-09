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

package com.dataintuitive.viash

import java.nio.file.Paths

import com.dataintuitive.viash.config._
import com.dataintuitive.viash.functionality.dataobjects.{FileObject, Output}
import com.dataintuitive.viash.helpers.IO
import com.dataintuitive.viash.helpers.Circe.{OneOrMore, One, More}

import scala.sys.process.{Process, ProcessLogger}

object ViashRun {
  def apply(config: Config, args: Seq[String], keepFiles: Option[Boolean]) {
    val fun = config.functionality
    val dir = IO.makeTemp("viash_" + fun.name)

    val dirArg = FileObject(
      name = "--viash_tempdir_arg",
      direction = Output,
      default = One(dir)
    )
    val fun2 = fun.copy(
      dummy_arguments = List(dirArg)
    )

    // execute command, print everything to console
    var code = -1
    try {
      // write executable and resources to temporary directory
      IO.writeResources(fun2.resources, dir)

      // determine command
      val cmd =
        Array(Paths.get(dir.toString, fun2.name).toString) ++ args

      // execute command, print everything to console
      code = Process(cmd).!(ProcessLogger(println, println))
      System.exit(code)
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
