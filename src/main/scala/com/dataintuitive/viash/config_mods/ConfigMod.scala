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

package io.viash.config_mods

import io.circe.{ACursor, HCursor, FailedCursor, Json}

// define command
case class ConfigMods(
  commands: List[Command] = Nil,
  preparseCommands: List[Command] = Nil
) {
  def apply(json: Json, preparse: Boolean): Json = {
    val commandsToApply = if (preparse) preparseCommands else commands
    commandsToApply.foldLeft(json) {
      case (cur, cmd) => cmd.apply(cur)
    }
  }
}
