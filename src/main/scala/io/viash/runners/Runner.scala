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

package io.viash.runners

import io.viash.schemas._
import io.viash.functionality.Functionality
import io.viash.config.Config
import io.viash.platforms.Platform

@description(
  """A list of runners to generate target artifacts for.
    |
    | * @[Executable](executable_runner)
    | * @[Nextflow](nextflow_runner)
    |""".stripMargin)
@example(
  """runners:
    |  - type: executable
    |  - type: nextflow
    |""".stripMargin,
  "yaml")
@subclass("ExecutableRunner")
@subclass("NextflowRunner")
trait Runner {
  @description("Specifies the type of the platform.")
  val `type`: String

  @description("Id of the runner.")
  @example("id: foo", "yaml")
  val id: String

  def generateRunner(config: Config, testing: Boolean): RunnerResources
}
