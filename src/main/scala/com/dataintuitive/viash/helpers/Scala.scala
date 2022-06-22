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

package com.dataintuitive.viash.helpers

object Scala {
  implicit class AugmentedOption[T](opt: Option[T]) {
    def `|`(other: Option[T]): Option[T] = {
      if (opt.isDefined) {
        opt
      } else {
        other
      }
    }
  }
}

import scala.annotation.meta._

@getter @setter @beanGetter @beanSetter @field
class since(since: String) extends scala.annotation.StaticAnnotation

@getter @setter @beanGetter @beanSetter @field
class example(example: String, format: String = "") extends scala.annotation.StaticAnnotation
