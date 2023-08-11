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

abstract class PathExp {
  def applyCommand(cursor: ACursor, cmd: ACursor => ACursor, remaining: Path, rewriteHistory: Boolean): ACursor
  def get(cursor: ACursor, remaining: Path): Json
}
case object Root extends PathExp {
  def applyCommand(cursor: ACursor, cmd: ACursor => ACursor, remaining: Path, rewriteHistory: Boolean): ACursor = {
    val parent = cursor.up
    if (parent.failed) {
      remaining.applyCommand(cursor, cmd, rewriteHistory)
      // todo: go back down again?
    } else {
      applyCommand(parent, cmd, remaining, rewriteHistory)
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
  def applyCommand(cursor: ACursor, cmd: ACursor => ACursor, remaining: Path, rewriteHistory: Boolean): ACursor = {
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
    val result = remaining.applyCommand(newCursor, cmd, rewriteHistory)
    result.up.success.getOrElse(result)
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
  def applyCommand(cursor: ACursor, cmd: ACursor => ACursor, remaining: Path, rewriteHistory: Boolean): ACursor = {
    def recurseApply(cursor: ACursor, cmd: ACursor => ACursor, remaining: Path, rewriteHistory: Boolean): ACursor = {
      val isLast = cursor.right.failed
      val (modifiedCursor, moveRight) =
        if (condition.apply(cursor.focus.get)) {
          val modified = remaining.applyCommand(cursor, cmd, rewriteHistory)
          val modifiedWithCorrectHistory = 
            if (rewriteHistory && !isLast) {
              // replay history of the original cursor on the modified cursor to make sure we're at the right position
              modified.top.get.hcursor.replay(cursor.history)
            } else {
              modified
            }
          (modifiedWithCorrectHistory, !remaining.path.isEmpty || !rewriteHistory)
        } else {
          (cursor, true)
        }
      
      val newCursor = 
        if (moveRight && !isLast)
          modifiedCursor.right
        else
          modifiedCursor

      if (!isLast)
        recurseApply(newCursor, cmd, remaining, rewriteHistory)
      else
        newCursor
    }

    val elemCursor = cursor.downArray
    if (elemCursor.failed) {
      // cursor doesn't have any children
      return cursor
    }
    val elemCursor2 = recurseApply(elemCursor, cmd, remaining, rewriteHistory)
    elemCursor2.up.success.getOrElse(elemCursor2)
  }

  def get(cursor: ACursor, remaining: Path): Json = {
    var elemCursor = cursor.downArray
    var lastWorking = elemCursor
    val out = new scala.collection.mutable.ListBuffer[Json]()
    while (!elemCursor.failed) {
      if (condition.apply(elemCursor.focus.get)) {
        out += remaining.get(elemCursor)
      }
      lastWorking = elemCursor
      elemCursor = elemCursor.right
    }
    Json.fromValues(out)
  }
}