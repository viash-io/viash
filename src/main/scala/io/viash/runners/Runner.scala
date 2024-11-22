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
import io.viash.config.Config

@description(
  """A list of runners to generate target artifacts for.
    |
    |A runner is what will be used to call using input parameters, execute the component, and afterwards use the output results.
    |
    | * @[Executable](executable_runner)
    | * @[Nextflow](nextflow_runner)
    |""")
@example(
  """runners:
    |  - type: executable
    |  - type: nextflow
    |""",
  "yaml")
@subclass("ExecutableRunner")
@subclass("NextflowRunner")
trait Runner {
  @description("Specifies the type of the runner.")
  val `type`: String

  @description("Id of the runner.")
  @example("id: foo", "yaml")
  val id: String

  def generateRunner(config: Config, testing: Boolean): RunnerResources
}
