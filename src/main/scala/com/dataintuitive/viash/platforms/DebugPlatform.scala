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

package com.dataintuitive.viash.platforms

import com.dataintuitive.viash.config.Config
import com.dataintuitive.viash.functionality.Functionality
import com.dataintuitive.viash.functionality.resources._
import com.dataintuitive.viash.platforms.requirements._
import com.dataintuitive.viash.config.Version
import com.dataintuitive.viash.helpers.Circe.One
import com.dataintuitive.viash.wrapper.BashWrapper
import com.dataintuitive.viash.functionality.dataobjects._
import java.nio.file.Path
import java.nio.file.Paths

// A platform solely for running `viash config inject` with.
case class DebugPlatform(
  id: String = "debug",
  `type`: String = "debug",
  path: String
) extends Platform {
  def modifyFunctionality(config: Config): Functionality = {
    val functionality = config.functionality
    if (functionality.mainScript.isEmpty) {
      throw new RuntimeException("Can't generate a debug platform when there is no script.")
    }

    // disable required arguments and set defaults for all arguments
    def mapArgs(args: List[DataObject[_]]) = {
      args.map {
        case arg if arg.required && arg.default.nonEmpty => 
          arg.copyDO(required = false)
        case arg if arg.default.isEmpty && arg.example.nonEmpty => 
          arg.copyDO(required = false, default = arg.example)
        case arg: BooleanObject if arg.default.isEmpty => 
          arg.copyDO(required = false, default = One(true))
        case arg: DoubleObject if arg.default.isEmpty => 
          arg.copy(required = false, default = One(123.0), min = None, max = None)
        case arg: FileObject if arg.default.isEmpty => 
          arg.copy(required = false, default = One(Paths.get("/path/to/file")), must_exist = false)
        case arg: IntegerObject if arg.default.isEmpty =>
           arg.copy(required = false, default = One(123), choices = Nil, min = None, max = None)
        case arg: StringObject if arg.default.isEmpty => 
          arg.copy(required = false, default = One("value"), choices = Nil)
        case a => a
      }
    }
    val fun0 = functionality.copy(
      inputs = mapArgs(functionality.inputs),
      outputs = mapArgs(functionality.outputs),
      arguments = mapArgs(functionality.arguments)
    )

    // create new bash script
    val bashScript = BashScript(
      dest = Some(functionality.name),
      text = Some(BashWrapper.wrapScript(
        executor = "bash",
        functionality = fun0,
        debugPath = Some(path)
      ))
    )
    fun0.copy(
      resources = List(bashScript)
    )
  }
}
