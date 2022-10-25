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
}
case class One[A](element: A) extends OneOrMore[A] {
  def toList = List(element)
}
case class More[A](list: List[A]) extends OneOrMore[A] {
  def toList = list
}
