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

package io.viash.wrapper

import io.viash.functionality.arguments.Argument

case class BashWrapperMods(
  preParse: String = "",
  parsers: String = "",
  postParse: String = "",
  preRun: String = "",
  postRun: String = "",
  last: String = "",
  extraParams: String = ""
) {
  def `++`(other: BashWrapperMods): BashWrapperMods = {
    BashWrapperMods(
      preParse = BashWrapper.joinSections(List(preParse, other.preParse)),
      parsers = BashWrapper.joinSections(List(parsers, other.parsers), middle = "\n"),
      postParse = BashWrapper.joinSections(List(postParse, other.postParse)),
      preRun = BashWrapper.joinSections(List(preRun, other.preRun)),
      postRun = BashWrapper.joinSections(List(postRun, other.postRun)),
      last = BashWrapper.joinSections(List(last, other.last)),
      extraParams = extraParams + other.extraParams
    )
  }
}
