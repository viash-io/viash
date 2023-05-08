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

package io.viash.functionality.resources

import io.viash.functionality._
import io.viash.helpers.Bash
import io.viash.functionality.arguments._
import io.viash.wrapper.BashWrapper
import io.viash.schemas._

import java.net.URI

@description("""An executable R script.
               |When defined in functionality.resources, only the first entry will be executed when running the built component or when running `viash run`.
               |When defined in functionality.test_resources, all entries will be executed during `viash test`.""".stripMargin)
case class RScript(
  path: Option[String] = None,
  text: Option[String] = None,
  dest: Option[String] = None,
  is_executable: Option[Boolean] = Some(true),
  parent: Option[URI] = None,

  @description("Specifies the resource as a R script.")
  `type`: String = RScript.`type`
) extends Script {
  val companion = RScript
  def copyResource(path: Option[String], text: Option[String], dest: Option[String], is_executable: Option[Boolean], parent: Option[URI]): Resource = {
    copy(path = path, text = text, dest = dest, is_executable = is_executable, parent = parent)
  }

  def generateInjectionMods(argsAndMeta: Map[String, List[Argument[_]]]): ScriptInjectionMods = {
    val paramsCode = argsAndMeta.map { case (dest, params) =>

      val parSet = params.map { par =>

        // todo: escape multiple_sep?
        val (lhs, rhs) = par match {
          case a: BooleanArgumentBase if a.multiple =>
            ("as.logical(strsplit(toupper(", s"), split = '${a.multiple_sep}')[[1]])")
          case a: IntegerArgument if a.multiple =>
            ("as.integer(strsplit(", s", split = '${a.multiple_sep}')[[1]])")
          case a: LongArgument if a.multiple =>
            ("bit64::as.integer64(strsplit(", s", split = '${a.multiple_sep}')[[1]])")
          case a: DoubleArgument if a.multiple =>
            ("as.numeric(strsplit(", s", split = '${a.multiple_sep}')[[1]])")
          case a: FileArgument if a.multiple =>
            ("strsplit(", s", split = '${a.multiple_sep}')[[1]]")
          case a: StringArgument if a.multiple =>
            ("strsplit(", s", split = '${a.multiple_sep}')[[1]]")
          case _: BooleanArgumentBase => ("as.logical(toupper(", "))")
          case _: IntegerArgument => ("as.integer(", ")")
          case _: LongArgument => ("bit64::as.integer64(", ")")
          case _: DoubleArgument => ("as.numeric(", ")")
          case _: FileArgument => ("", "")
          case _: StringArgument => ("", "")
        }
        val sl = "\\VIASH_SLASH\\" // used instead of "\\", as otherwise the slash gets escaped automatically.

        val notFound = "NULL"
        
        s""""${par.plainName}" = $$VIASH_DOLLAR$$( if [ ! -z $${${par.VIASH_PAR}+x} ]; then echo -n "$lhs'"; echo -n "$$${par.VIASH_PAR}" | sed "s#['$sl$sl]#$sl$sl$sl$sl&#g"; echo "'$rhs"; else echo $notFound; fi )"""
      }

      s"""$dest <- list(
        |  ${parSet.mkString(",\n  ")}
        |)
        |""".stripMargin
    }

    val outCode = s"""# treat warnings as errors
       |.viash_orig_warn <- options(warn = 2)
       |
       |${paramsCode.mkString}
       |
       |# restore original warn setting
       |options(.viash_orig_warn)
       |rm(.viash_orig_warn)
       |""".stripMargin
    ScriptInjectionMods(params = outCode)
  }

  def command(script: String): String = {
    "Rscript \"" + script + "\""
  }

  def commandSeq(script: String): Seq[String] = {
    Seq("Rscript", script)
  }
}

object RScript extends ScriptCompanion {
  val commentStr = "#"
  val extension = "R"
  val `type` = "r_script"
}