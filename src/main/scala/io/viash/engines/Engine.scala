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

package io.viash.engines

import io.viash.schemas._

@description(
  """A list of engines to generate target artifacts for.
    |
    | * @[Docker](docker_engine)
    | * @[Native](native_engine)
    |""".stripMargin)
@example(
  """engines:
    |  - type: docker
    |    image: "bash:4.0"
    |  - type: native
    |""".stripMargin,
  "yaml")
@subclass("DockerEngine")
@subclass("NativeEngine")
trait Engine {
  @description("Specifies the type of the engine.")
  val `type`: String

  @description("Id of the engine.")
  @example("id: foo", "yaml")
  val id: String

  @internalFunctionality
  val hasSetup: Boolean
}
