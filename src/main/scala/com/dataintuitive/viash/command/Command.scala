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

package com.dataintuitive.viash.command

import io.circe.{ACursor, FailedCursor, Json}

// define command
case class Block(commands: List[Command]) {
  def apply(cursor: ACursor): ACursor = {
    commands.foldLeft(cursor) {
      case (cur, cmd) => cmd.apply(cur)
    }
  }
}
case class Command(path: Path, op: CommandExp) {
  def apply(cursor: ACursor): ACursor = {
    val comb = Path(path.path ::: List(op))
    comb.apply(cursor).top.get.hcursor
  }
}
abstract class CommandExp extends PathExp {
  def command(cursor: ACursor): ACursor

  // tail should always be Path(Nil)
  def apply(cursor: ACursor, tail: Path): ACursor = {
    command(cursor)
  }
}
case class Modify(value: Json) extends CommandExp {
  def command(cursor: ACursor): ACursor = {
    cursor.set(value)
  }
}
case class Add(value: Json) extends CommandExp {
  def command(cursor: ACursor): ACursor = {
    cursor.withFocus{js =>
      Json.fromValues(js.asArray.get ++ Array(value)) // TODO: will error if get fails
    }
  }
}


// define values
abstract class Value {
  def get(cursor: ACursor): Option[Json]
}
case class JsonValue(value: Json) extends Value {
  def get(cursor: ACursor): Option[Json] = Some(value)
}

// define paths
case class Path(path: List[PathExp]) extends Value {
  def apply(cursor: ACursor): ACursor = {
    path match {
      case Nil => cursor
      case head :: tail => {
        head.apply(cursor, Path(tail))
      }
    }
  }
  // assume value is a json which might not be desirable
  def get(cursor: ACursor): Option[Json] = {
    apply(cursor).focus
  }
}
abstract class PathExp {
  def apply(cursor: ACursor, tail: Path): ACursor
}
case object Root extends PathExp {
  def getRoot(cursor: ACursor): ACursor = {
    cursor.up match {
      case _: FailedCursor => cursor
      case parent: ACursor if parent.succeeded => getRoot(parent)
    }
  }

  def apply(cursor: ACursor, tail: Path): ACursor = {
    val root = getRoot(cursor)
    tail.apply(root)
  }
}
case object Parent extends PathExp {
  def apply(cursor: ACursor, tail: Path): ACursor = {
    val parent = cursor.up
    tail.apply(parent)
  }
}
case class Attribute(string: String) extends PathExp {
  def apply(cursor: ACursor, tail: Path): ACursor = {
    val down = cursor.downField(string)
    tail.apply(down)
  }
}
case class Filter(condition: Condition) extends PathExp {
  def apply(cursor: ACursor, tail: Path): ACursor = {
    var elemCursor = cursor.downArray
    var lastWorking = elemCursor
    while (!elemCursor.failed) {
      if (condition.apply(elemCursor)) {
        val elemModified = tail.apply(elemCursor)
        // replay history of elemCursor on elemModified to make sure we're at the right position
        elemCursor = elemModified.top.get.hcursor.replay(elemCursor.history)
      }
      lastWorking = elemCursor
      elemCursor = elemCursor.right
    }
    lastWorking
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
case class NotEquals(left: Value, right: Value) extends Condition {
  def apply(cursor: ACursor): Boolean = {
    left.get(cursor) != right.get(cursor)
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

