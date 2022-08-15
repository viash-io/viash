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

// define condition ops
abstract class Condition {
  def apply(json: Json): Boolean
}
case object True extends Condition {
  def apply(json: Json) = true
}
case object False extends Condition {
  def apply(json: Json) = false
}
case class Equals(left: Value, right: Value) extends Condition {
  def apply(json: Json): Boolean = {
    left.get(json) == right.get(json)
  }
}
case class NotEquals(left: Value, right: Value) extends Condition {
  def apply(json: Json): Boolean = {
    left.get(json) != right.get(json)
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
  def apply(json: Json): Boolean = {
    left(json) && right(json)
  }
}
case class Or(left: Condition, right: Condition) extends Condition {
  def apply(json: Json): Boolean = {
    left(json) || right(json)
  }
}
case class Not(value: Condition) extends Condition {
  def apply(json: Json): Boolean = {
    !value(json)
  }
}