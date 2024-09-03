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

import io.viash.schemas._
import io.viash.engines.requirements.Requirements

@description(
  """A list of platforms to generate target artifacts for.
    |
    | * @[Native](platform_native)
    | * @[Docker](platform_docker)
    | * @[Nextflow](platform_nextflow)
    |""".stripMargin)
@example(
  """platforms:
    |  - type: docker
    |    image: "bash:4.0"
    |  - type: native
    |  - type: nextflow
    |    directives:
    |      label: [lowcpu, midmem]
    |""".stripMargin,
  "yaml")
@deprecated("Use 'engines' and 'runners' instead.", "0.9.0", "0.10.0")
@subclass("NativePlatform")
@subclass("DockerPlatform")
@subclass("NextflowPlatform")
@deprecated("Use 'engines' and 'runners' instead.", "0.9.0", "0.10.0")
trait Platform {
  @description("Specifies the type of the platform.")
  val `type`: String
  
  val id: String

  val requirements: List[Requirements] = Nil
}
