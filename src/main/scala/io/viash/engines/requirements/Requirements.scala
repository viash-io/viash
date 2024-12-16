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

package io.viash.engines.requirements

import io.viash.schemas._

@description(
  """Requirements for installing the following types of packages:
    |
    | - @[apt](apt_req)
    | - @[apk](apk_req)
    | - @[Docker setup instructions](docker_req)
    | - @[JavaScript](javascript_req)
    | - @[Python](python_req)
    | - @[R](r_req)
    | - @[Ruby](ruby_req)
    | - @[yum](yum_req)
    |""")
@subclass("ApkRequirements")
@subclass("AptRequirements")
@subclass("DockerRequirements")
@subclass("JavaScriptRequirements")
@subclass("PythonRequirements")
@subclass("RRequirements")
@subclass("RubyRequirements")
@subclass("YumRequirements")
trait Requirements {
  @description("Specifies the type of the requirement specification.")
  val `type`: String

  def installCommands: List[String]

  def dockerCommands: Option[String] = {
    if (installCommands.isEmpty) {
      None
    } else {
      Some(installCommands.mkString("RUN ", " && \\\n  ", "\n"))
    }
  }
}