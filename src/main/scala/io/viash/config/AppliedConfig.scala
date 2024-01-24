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

import io.viash.runners.Runner
import io.viash.helpers.status.Status
import io.viash.engines.Engine

final case class AppliedConfig(
  config: Config,
  runner: Option[Runner],
  engines: List[Engine],

  status: Option[Status] // None if still processing, Some if failed or fully finished building, running, ...
) {
  def validForBuild = runner.isDefined && engines.nonEmpty
  def generateRunner(testing: Boolean) = {
    // override the config engines with the selected engines
    // TODO: is this ok?
    val config2 = config.copy(engines = engines)

    // TODO: check if runner is defined, and fail gracefully if it isn't
    runner.get.generateRunner(config2, testing)
  }
  def setStatus(status: Status) = copy(status = Some(status))
}

object AppliedConfig {
  implicit def fromConfig(config: Config): AppliedConfig = {
    AppliedConfig(config, None, Nil, None)
  }
}

