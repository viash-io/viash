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

object Bash {
  private def readUtils(s: String) = {
    val path = s"io/viash/helpers/bashutils/$s.sh"
    Source.fromResource(path).getLines().mkString("\n")
  }

  lazy val ViashQuote: String = readUtils("ViashQuote")
  lazy val ViashExtractFlags: String = readUtils("ViashExtractFlags")
  lazy val ViashRemoveFlags: String = readUtils("ViashRemoveFlags")
  lazy val ViashAbsolutePath: String = readUtils("ViashAbsolutePath")
  lazy val ViashAutodetectMount: String = readUtils("ViashAutodetectMount")
  lazy val ViashSourceDir: String = readUtils("ViashSourceDir")
  lazy val ViashDockerFuns: String = readUtils("ViashDockerFuns")
  lazy val ViashLogging: String = readUtils("ViashLogging")

  def save(saveVariable: String, args: Seq[String]): String = {
    saveVariable + "=\"$" + saveVariable + " " + args.mkString(" ") + "\""
  }

  // generate strings in the form of:
  // SAVEVARIABLE="$SAVEVARIABLE $(Quote arg1) $(Quote arg2)"
  def quoteSave(saveVariable: String, args: Seq[String]): String = {
    saveVariable + "=\"$" + saveVariable +
      args.map(" $(ViashQuote \"" + _ + "\")").mkString +
      "\""
  }

  def argStore(name: String, plainName: String, store: String, argsConsumed: Int, storeUnparsed: Option[String]): String = {
    val passStr =
      if (storeUnparsed.isDefined) {
        "\n            " + quoteSave(storeUnparsed.get, (1 to argsConsumed).map("$" + _))
      } else {
        ""
      }
    s"""         $name)
       |            $plainName=$store$passStr
       |            shift $argsConsumed
       |            ;;""".stripMargin
  }

  def argStoreSed(name: String, plainName: String, storeUnparsed: Option[String]): String = {
    argStore(name + "=*", plainName, "$(ViashRemoveFlags \"$1\")", 1, storeUnparsed)
  }

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
  def getEscapedArgument(env: String, quot: String, from: String, to: String) = {
    s"$quot$${$env//$from/$to}$quot".replaceAll("\\\\", "\\\\VIASH_SLASH\\\\")
  }

  case class Escaper(str: String) {
    def transform(fun: Function[String, String], apply: Boolean = true): Escaper = {
      Escaper(if (apply) fun(str) else str)
    }
  }

  def escape(
    str: String,
    backtick: Boolean = true,
    quote: Boolean = false,
    singleQuote: Boolean = false,
    newline: Boolean = false
  ): String = {
    val x = Escaper(str)
      .transform(_.replaceAll("([\\\\$])", "\\\\$1"))
      .transform(_.replaceAll("`", "\\\\`"), backtick)
      .transform(_.replaceAll("\"", "\\\\\""), quote)
      .transform(_.replaceAll("'", "\\\\'"), singleQuote)
      .transform(_.replaceAll("\n", "\\\\n"), newline)
    x.str
  }

  def escapeMore(str: String, quote: Boolean = false, newline: Boolean = false): String = {
    escape(str, quote = quote, newline = newline)
      .replaceAll("\\\\\\$VIASH_DOLLAR\\\\\\$", "\\$")
      .replaceAll("\\\\\\\\VIASH_SLASH\\\\\\\\", "\\\\")
      .replaceAll("\\\\\\$VIASH_", "\\$VIASH_")
      .replaceAll("\\\\\\$\\{VIASH_", "\\${VIASH_")
  }
}
