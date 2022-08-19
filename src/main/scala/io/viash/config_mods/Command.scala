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

import io.circe.Json

// A class inheriting from Command can be applied to a Json to generate another Json
abstract class Command {
  def apply(json: Json): Json
}


case class Assign(lhs: Path, rhs: Value) extends Command {
  def apply(json: Json): Json = {
    val result = rhs.get(json)
    lhs.applyCommand(json, { _.set(result) })
  }
}
case class Delete(path: Path) extends Command {
  def apply(json: Json): Json = {
    path.applyCommand(json, { _.delete })
  }
}
case class Append(lhs: Path, rhs: Value) extends Command {
  def apply(json: Json): Json = {
    val result = rhs.get(json)
    val resultVector = 
      if (result.isArray) {
        result.asArray.get
      } else {
        Vector(result)
      }
    lhs.applyCommand(json, { cursor =>
      cursor.withFocus { cursorJson => 
        cursorJson.mapArray(_ ++ resultVector)
      }
    })
  }
}
case class Prepend(lhs: Path, rhs: Value) extends Command {
  def apply(json: Json): Json = {
    val result = rhs.get(json)
    val resultVector = 
      if (result.isArray) {
        result.asArray.get
      } else {
        Vector(result)
      }
    lhs.applyCommand(json, { cursor =>
      cursor.withFocus { cursorJson => 
        cursorJson.mapArray(resultVector ++ _)
      }
    })
  }
}


