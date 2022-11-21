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
import io.viash.helpers.data_structures._
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
            // set required to false
            .map{
              case arg =>
                arg.copyArg(required = false)
            }
            // make sure the default is set. invent one if necessary.s
            .map{
              // if available, use default
              case arg if arg.default.nonEmpty => 
                arg
              // else use example
              case arg if arg.example.nonEmpty => 
                arg.copyArg(default = arg.example)
              // else invent one
              case arg: BooleanArgumentBase => 
                arg.copyArg(default = OneOrMore(true))
              case arg: DoubleArgument => 
                arg.copy(default = OneOrMore(123.0), min = None, max = None)
              case arg: FileArgument => 
                arg.copy(default = OneOrMore(Paths.get("/path/to/file")), must_exist = false)
              case arg: IntegerArgument =>
                arg.copy(default = OneOrMore(123), choices = Nil, min = None, max = None)
              case arg: LongArgument =>
                arg.copy(default = OneOrMore(123), choices = Nil, min = None, max = None)
              case arg: StringArgument => 
                arg.copy(default = OneOrMore("value"), choices = Nil)
              case arg => 
                throw new RuntimeException(
                  f"Viash is missing a default for argument of type ${arg.`type`}. " +
                    "Please report this error to the maintainers.")
            }
            // turn off must_exist and create_parent
            .map{
              case arg: FileArgument =>
                arg.copy(must_exist = false, create_parent = false)
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
    val scriptSrc = BashWrapper.wrapScript(
      executor = "bash",
      functionality = fun0,
      debugPath = Some(path)
    )
    val bashScript = BashScript(
      dest = Some(functionality.name),
      text = Some(scriptSrc)
    )
    fun0.copy(
      resources = List(bashScript)
    )
  }
}
