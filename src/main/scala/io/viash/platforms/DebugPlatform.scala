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
    // create new bash script
    // by setting debugpath, any checks on the arguments are getting disabled
    // TODO: enforce this behaviour using a `disableChecks = true` argument?
    val scriptSrc = BashWrapper.wrapScript(
      executor = "bash",
      functionality = functionality,
      debugPath = Some(path),
      config = config
    )
    val bashScript = BashScript(
      dest = Some(functionality.name),
      text = Some(scriptSrc)
    )
    config.functionality.copy(
      resources = List(bashScript)
    )
  }
}
