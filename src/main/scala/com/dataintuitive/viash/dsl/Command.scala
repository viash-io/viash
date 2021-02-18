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

package com.dataintuitive.viash.dsl

import io.circe.syntax.EncoderOps
import io.circe.{ACursor, FailedCursor, Json}

// define values
abstract class Value {
  def get(cursor: ACursor): Option[Json]
}
case class Literal(value: String) extends Value {
  def get(cursor: ACursor): Option[Json] = {
    Some(value.asJson)
  }
}

// define paths
case class Path(path: List[PathExp]) extends Value {
  def apply(cursor: ACursor): ACursor = {
    path.foldLeft(cursor) { (cursor, pathexp) =>
      pathexp.apply(cursor)
    }
  }
  // assume value is a string, which is definitely not desirable
  def get(cursor: ACursor): Option[Json] = {
    apply(cursor).focus
  }
}
abstract class PathExp {
  def apply(cursor: ACursor): ACursor
}
case object Root extends PathExp {
  def apply(cursor: ACursor): ACursor = {
    cursor.up match {
      case _: FailedCursor => cursor
      case parent: ACursor => parent
    }
  }
}
case class Attribute(string: String) extends PathExp {
  def apply(cursor: ACursor): ACursor = {
    cursor.downField(string)
  }
}
case class Filter(condition: Condition) extends PathExp {
  def apply(cursor: ACursor): ACursor = {
    var elemCursor = cursor.downArray
    // TODO: should be able to apply on multiple things
    // with this implementation, commands are only applied on the first matching element
    while (!elemCursor.failed && !condition.apply(elemCursor)) {
      elemCursor = elemCursor.right
    }
    elemCursor
  }
}

// define condition ops
abstract class Condition {
  def apply(cursor: ACursor): Boolean
}
case object True extends Condition {
  def apply(cursor: ACursor) = true
}
case object False extends Condition {
  def apply(cursor: ACursor) = false
}
case class Equals(left: Value, right: Value) extends Condition {
  def apply(cursor: ACursor): Boolean = {
    left.get(cursor) == right.get(cursor)
  }
}
case class And(left: Condition, right: Condition) extends Condition {
  def apply(cursor: ACursor): Boolean = {
    left(cursor) && right(cursor)
  }
}
case class Or(left: Condition, right: Condition) extends Condition {
  def apply(cursor: ACursor): Boolean = {
    left(cursor) || right(cursor)
  }
}
case class Not(value: Condition) extends Condition {
  def apply(cursor: ACursor): Boolean = {
    !value(cursor)
  }
}

// define command
abstract class Command {
  def apply(cursor: ACursor): ACursor
}
case class Modify(path: Path, value: Json) extends Command {
  def apply(cursor: ACursor): ACursor = {
    path.apply(cursor).set(value)
  }
}
case class Add(path: Path, value: Json) extends Command {
  def apply(cursor: ACursor): ACursor = {
    path.apply(cursor).withFocus{js =>
      Json.fromValues(js.asArray.get ++ Array(value)) // TODO: will error if get fails
    }
  }
}


