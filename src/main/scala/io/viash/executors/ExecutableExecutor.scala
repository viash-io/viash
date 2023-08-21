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

package io.viash.executors

import io.viash.schemas._
import io.viash.config.Config

final case class ExecutableExecutor(
  @description("Name of the executor. As with all executors, you can give an executor a different name. By specifying `id: foo`, you can target this executor (only) by specifying `...` in any of the Viash commands.")
  @example("id: foo", "yaml")
  @default("executable")
  id: String = "executable",
) extends Executor {
  val `type` = "executable"

  def generateExecutor(config: Config, testing: Boolean): ExecutorResources = {
    val containers = config.getContainers
    
    // todo: do something with containers

    // todo: generate mainscript
    val mainScript = None
    val additionalResources = Nil

    // return output
    ExecutorResources(
      mainScript = mainScript,
      additionalResources = additionalResources
    )
  }
}
