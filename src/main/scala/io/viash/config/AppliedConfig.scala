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

package io.viash.config

import io.viash.executors.Executor
import io.viash.platforms.Platform
import io.viash.helpers.status.Status

final case class AppliedConfig(
  config: Config,
  executor: Option[Executor],
  platform: Option[Platform],

  status: Option[Status] // None if still processing, Some if failed or fully finished building, running, ...
) {
  def validForBuild = executor.isDefined && platform.isDefined
  def generateExecutor(testing: Boolean) = executor.get.generateExecutor(config, testing)
  def setStatus(status: Status) = copy(status = Some(status))
}

object AppliedConfig {
  implicit def fromConfig(config: Config) = AppliedConfig(config, None, None, None)
}

