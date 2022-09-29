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

package io.viash.helpers



object Escaper {
  /**
    * Helper class which allows escaping characters conditionally  
    *
    * @param str String to be escaped
    */ 
  implicit class EscaperHelper(str: String) {
    def escapeWith(fun: Function[String, String], condition: Boolean = true): String = {
      if (condition) {
        fun(str)
      } else {
        str
      }
    }

    def replaceWith(arg0: String, arg1: String, condition: Boolean = true): String = {
      if (condition) {
        str.replaceAll(arg0, arg1)
      } else {
        str
      }
    }
  }

  /**
   * Escape specific characters in 'str'.
   */
  def apply(
    str: String,
    slash: Boolean = false,
    dollar: Boolean = false,
    backtick: Boolean = false,
    quote: Boolean = false,
    singleQuote: Boolean = false,
    newline: Boolean = false
  ): String = {
    str
      .replaceWith("\\\\", "\\\\\\\\", condition = slash)
      .replaceWith("\\$", "\\\\\\$", condition = dollar)
      .replaceWith("`", "\\\\`", condition = backtick)
      .replaceWith("\"", "\\\\\"", condition = quote)
      .replaceWith("'", "\\\\'", condition = singleQuote)
      .replaceWith("\n", "\\\\n", condition = newline)
  }
}