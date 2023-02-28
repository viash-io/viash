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

package io.viash.helpers.data_structures

// case class OneOrMore[A](list: A*) {
//   def toList = list.toList
//   override def toString = list.toList.toString().replaceFirst("List", "OneOrMore")
// }

// oneormore helper type
abstract class OneOrMore[+A] {
  def toList: List[A]
  override def equals(that: Any): Boolean = {
    that match {
      case that: OneOrMore[_] => {
        this.toList.equals(that.toList)
      }
      case _ => false
    }
  }
  override def toString = this.toList.toString.replaceFirst("List", "OneOrMore")
  val isDefined: Boolean
}
object OneOrMore {
  def apply[A](list: A*) = More(list.toList)
}
case object Zero extends OneOrMore[Nothing] {
  def toList = Nil
  val isDefined = false
}
case class One[A](element: A) extends OneOrMore[A] {
  def toList = List(element)
  val isDefined = true
}
case class More[A](list: List[A]) extends OneOrMore[A] {
  def toList = list
  val isDefined = true
}