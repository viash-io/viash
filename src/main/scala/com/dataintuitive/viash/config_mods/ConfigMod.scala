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

// A class inheriting from Command can be applied to a Json to generate another Json
abstract class Command {
  def apply(json: Json): Json
}

// A class inheriting from Value can be used to extract a Json from another Json.
abstract class Value {
  def get(json: Json): Json = {
    get(json.hcursor)
  }
  def get(cur: ACursor): Json
}

case class Assign(lhs: Path, rhs: Value) extends Command {

  def apply(json: Json): Json = {
    val result = rhs.get(json)
    lhs.applyCommand(json, { _.set(result) })
  }
}
// case class ModifyPath(path: Path) extends CommandExp {
//   def command(cursor: ACursor): ACursor = {
//     val value = path.get(cursor.top.get.hcursor).get
//     Modify(value).command(cursor)
//   }
// }
// case object Delete extends CommandExp {
//   def command(cursor: ACursor): ACursor = {
//     cursor.delete
//   }
// }
// case class Add(value: Json) extends CommandExp {
//   def command(cursor: ACursor): ACursor = {
//     if (value.isArray)
//       cursor.withFocus(_.mapArray(_ ++ value.asArray.get))
//     else
//       cursor.withFocus(_.mapArray(_ ++ Vector(value)))
//     // cursor.withFocus{js =>
//     //   Json.fromValues(js.asArray.get ++ Array(value)) // TODO: will error if get fails
//     // }
//   }
// }
// case class AddPath(path: Path) extends CommandExp {
//   def command(cursor: ACursor): ACursor = {
//     val value = path.get(cursor.top.get.hcursor).get
//     println(s"AddPath $path $value")
//     Add(value).command(cursor)
//   }
// }
// case class Prepend(value: Json) extends CommandExp {
//   def command(cursor: ACursor): ACursor = {
//     cursor.withFocus(_.mapArray(Vector(value) ++ _))
//     // cursor.withFocus{js =>
//     //   Json.fromValues(Array(value) ++ js.asArray.get) // TODO: will error if get fails
//     // }
//   }
// }


// define values

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
abstract class PathExp {
  def applyCommand(cursor: ACursor, cmd: ACursor => ACursor, remaining: Path): ACursor
  def get(cursor: ACursor, remaining: Path): Json
}
case object Root extends PathExp {
  def applyCommand(cursor: ACursor, cmd: ACursor => ACursor, remaining: Path): ACursor = {
    val parent = cursor.up
    if (parent.failed) {
      remaining.applyCommand(cursor, cmd)
      // todo: go back down again?
    } else {
      applyCommand(parent, cmd, remaining)
    }
  }
  def get(cursor: ACursor, remaining: Path): Json = {
    val parent = cursor.up
    if (parent.failed) {
      remaining.get(cursor)
    } else {
      get(parent, remaining)
    }
  }
}
case class Attribute(string: String) extends PathExp {
  def applyCommand(cursor: ACursor, cmd: ACursor => ACursor, remaining: Path): ACursor = {
    val down = cursor.downField(string)
    val newCursor = 
      if (down.failed) {
        // create field if it doesn't exist
        val newDown = cursor
          .withFocus(_.mapObject(_.add(string, Json.Null)))
          .downField(string)
        newDown
      } else {
        down
      }
    val result = remaining.applyCommand(newCursor, cmd)
    val tryGoingUp = result.up
    if (tryGoingUp.failed) {
      // todo: going up should always work, so throw an error if it doesn't?
      result
    } else {
      tryGoingUp
    }
  }
  def get(cursor: ACursor, remaining: Path): Json = {
    val down = cursor.downField(string)
    if (down.failed) {
      Json.Null // todo: is it okay not to process the remaining path?
    } else {
      remaining.get(down)
    }
  }
}
case class Filter(condition: Condition) extends PathExp {
  def applyCommand(cursor: ACursor, cmd: ACursor => ACursor, remaining: Path): ACursor = {
    var elemCursor = cursor.downArray
    var lastWorking = elemCursor
    while (!elemCursor.failed) {
      if (condition.apply(elemCursor)) {
        val elemModified = remaining.applyCommand(elemCursor, cmd)
        // replay history of elemCursor on elemModified to make sure we're at the right position
        // elemCursor = elemModified.top.get.hcursor.replay(elemCursor.history)
        // todo: does this need to be re-enabled?
        elemCursor = elemModified
      }
      lastWorking = elemCursor
      elemCursor = elemCursor.right
    }
    val tryGoingUp = lastWorking.up
    if (tryGoingUp.failed) { // try to go back up
      lastWorking
    } else {
      tryGoingUp
    }
  }

  def get(cursor: ACursor, remaining: Path): Json = {
    var elemCursor = cursor.downArray
    var lastWorking = elemCursor
    val out = new scala.collection.mutable.ListBuffer[Json]()
    while (!elemCursor.failed) {
      if (condition.apply(elemCursor)) {
        out += remaining.get(elemCursor)
      }
      lastWorking = elemCursor
      elemCursor = elemCursor.right
    }
    Json.fromValues(out)
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
// case class Has(id: Value) extends Condition {
//   def apply(cursor: ACursor): Boolean = {
//     id.get(cursor) match {
//       case None => false
//       case Some(j: Json) => !j.isNull
//       case _ => true
//     }
//   }
// }
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

