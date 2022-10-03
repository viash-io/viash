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

package io.viash.platforms

import io.viash.config.Config
import io.viash.functionality.Functionality
import io.viash.functionality.resources._
import io.viash.platforms.requirements._
import io.viash.helpers.Circe.One
import io.viash.wrapper.BashWrapper
import io.viash.functionality.arguments._
import java.nio.file.Path
import java.nio.file.Paths

// A platform solely for running `viash config inject` with.
case class DebugPlatform(
  id: String = "debug",
  `type`: String = "debug",
  path: String
) extends Platform {
  def modifyFunctionality(config: Config, testing: Boolean): Functionality = {
    val functionality = config.functionality
    if (functionality.mainScript.isEmpty) {
      throw new RuntimeException("Can't generate a debug platform when there is no script.")
    }

    // disable required arguments and set defaults for all arguments
    val newArgumentGroups = functionality.allArgumentGroups.map { argument_group => 
        argument_group.copy(
          arguments = argument_group.arguments
            .flatMap(_.right.toOption) // there shouldn't be any lefts at this stage
            .map{
              case arg if arg.required && arg.default.nonEmpty => 
                arg.copyArg(required = false)
              case arg if arg.default.isEmpty && arg.example.nonEmpty => 
                arg.copyArg(required = false, default = arg.example)
              case arg: BooleanArgumentBase if arg.default.isEmpty => 
                arg.copyArg(required = false, default = One(true))
              case arg: DoubleArgument if arg.default.isEmpty => 
                arg.copy(required = false, default = One(123.0), min = None, max = None)
              case arg: FileArgument if arg.default.isEmpty => 
                arg.copy(required = false, default = One(Paths.get("/path/to/file")), must_exist = false)
              case arg: IntegerArgument if arg.default.isEmpty =>
                arg.copy(required = false, default = One(123), choices = Nil, min = None, max = None)
              case arg: LongArgument if arg.default.isEmpty =>
                arg.copy(required = false, default = One(123), choices = Nil, min = None, max = None)
              case arg: StringArgument if arg.default.isEmpty => 
                arg.copy(required = false, default = One("value"), choices = Nil)
              case a => a
            }
            .map{Right(_)}
        )
      }
    val fun0 = functionality.copy(
      inputs = Nil,
      outputs = Nil,
      arguments = Nil,
      argument_groups = newArgumentGroups
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
