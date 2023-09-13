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

trait Runner {
  @description("Specifies the type of the platform.")
  val `type`: String

  @description("Id of the runner.")
  @example("id: foo", "yaml")
  val id: String

  def generateRunner(config: Config, testing: Boolean): RunnerResources
}

object Runner{
  // Helper method to fascilitate conversion of legacy code to the new methods
  def get(platform: Platform) = {
    platform match {
      case p: Runner => p
      case _ => throw new RuntimeException("Expected all legacy platforms to be runners")
    }
  }
}

