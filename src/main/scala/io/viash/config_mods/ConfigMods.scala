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

import java.nio.file.{Files, Path => JPath}
import java.util.stream.Collectors
import io.circe.{ACursor, HCursor, FailedCursor, Json}

// define command
case class ConfigMods(
  postparseCommands: List[Command] = Nil,
  preparseCommands: List[Command] = Nil
) {
  def apply(json: Json, preparse: Boolean): Json = {
    val commandsToApply = if (preparse) preparseCommands else postparseCommands
    commandsToApply.foldLeft(json) {
      case (cur, cmd) => cmd.apply(cur)
    }
  }
  def `+`(cm: ConfigMods): ConfigMods = {
    ConfigMods(
      postparseCommands = postparseCommands ::: cm.postparseCommands,
      preparseCommands = preparseCommands ::: cm.preparseCommands
    )
  }
}

object ConfigMods {
  /**
    * Convert a string to a config mod
    *
    * @param li A list of strings to convert to config mods
    * @return A ConfigMods object
    */
  def parseConfigMods(li: List[String]): ConfigMods = {
    if (li.isEmpty) {
      ConfigMods()
    } else {
      ConfigModParser.block.parse(li.mkString("; "))
      // todo: allow for commands to span over multiple lines.
    }
  }
}
