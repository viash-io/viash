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

import scala.io.Source
import Escaper._

object Bash {
  private def readUtils(s: String) = {
    val path = s"io/viash/helpers/bashutils/$s.sh"
    Source.fromResource(path).getLines().mkString("\n")
  }

  lazy val ViashQuote: String = readUtils("ViashQuote")
  lazy val ViashRemoveFlags: String = readUtils("ViashRemoveFlags")
  lazy val ViashAbsolutePath: String = readUtils("ViashAbsolutePath")
  lazy val ViashDockerAutodetectMount: String = readUtils("ViashDockerAutodetectMount")
  lazy val ViashSourceDir: String = readUtils("ViashSourceDir")
  lazy val ViashFindTargetDir: String = readUtils("ViashFindTargetDir")
  lazy val ViashDockerFuns: String = readUtils("ViashDockerFuns")
  lazy val ViashLogging: String = readUtils("ViashLogging")
  lazy val ViashRenderYaml: String = readUtils("ViashRenderYaml")
  lazy val ViashParseArgumentValue: String = readUtils("ViashParseArgumentValue")

  /** 
   * Access the parameters contents in a bash script,
   * taking into account that some characters need to be escaped.
   * 
   * Example: Bash.getEscapedArgument("VIASH_PAR_MYSTRING", "'", """\'""", """\\\'""") 
   * results in '${VIASH_PAR_MYSTRING//\'/\\\'}'. 
   * 
   * Sidenote: a '\' will be escaped as '\VIASH_SLASH\', so BashWrapper
   * substitutes it back for a '\' instead of escaping it as a '\\'.
   */
  def getEscapedArgument(env: String, quot: String, from: String, to: String): String = {
    getEscapedArgument(env, quot, quot, from, to)
  }
  def getEscapedArgument(env: String, left: String, right: String, from: String, to: String): String = {
    s"$left$${$env//$from/$to}$right".replaceAll("\\\\", "\\\\VIASH_SLASH\\\\")
  }

  def escapeString(
    str: String, 
    quote: Boolean = false, 
    singleQuote: Boolean = false, 
    newline: Boolean = false,
    allowUnescape: Boolean = false
  ) = {
    Escaper(
      str,
      slash = true,
      dollar = true,
      backtick = true,
      quote = quote,
      singleQuote = singleQuote,
      newline = newline
    )
      .replaceWith("\\\\\\$VIASH_DOLLAR\\\\\\$", "\\$", allowUnescape)
      .replaceWith("\\\\\\\\VIASH_SLASH\\\\\\\\", "\\\\", allowUnescape)
      .replaceWith("\\\\\\$VIASH_", "\\$VIASH_", allowUnescape)
      .replaceWith("\\\\\\$\\{VIASH_", "\\${VIASH_", allowUnescape)
  }
}
