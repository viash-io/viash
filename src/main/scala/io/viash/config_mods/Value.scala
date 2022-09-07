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

import io.circe.{ACursor, Json}

// define values


// A class inheriting from Value can be used to extract a Json from another Json.
abstract class Value {
  def get(json: Json): Json = {
    get(json.hcursor)
  }
  def get(cur: ACursor): Json
}
case class JsonValue(value: Json) extends Value {
  def get(json: ACursor): Json = value
}

// define paths
case class Path(path: List[PathExp]) extends Value {
  def applyCommand(json: Json, cmd: ACursor => ACursor): Json = {
    applyCommand(json.hcursor, cmd).top.get
  }
  def applyCommand(cursor: ACursor, cmd: ACursor => ACursor): ACursor = {
    path match {
      case head :: tail => {
        head.applyCommand(cursor, cmd, Path(tail))
      }
      case Nil => cmd(cursor) // or throw error?
    }
  }
  def get(cursor: ACursor): Json = {
    path match {
      case head :: tail => {
        head.get(cursor, Path(tail))
      }
      case Nil => {
        cursor.focus match {
          case Some(js) => js
          case None => Json.Null // or throw error?
        }
      }
    }
  }
}

