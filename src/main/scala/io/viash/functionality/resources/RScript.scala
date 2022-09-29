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

import java.net.URI

case class RScript(
  path: Option[String] = None,
  text: Option[String] = None,
  dest: Option[String] = None,
  is_executable: Option[Boolean] = Some(true),
  parent: Option[URI] = None,
  entrypoint: Option[String] = None,
  `type`: String = RScript.`type`
) extends Script {
  assert(entrypoint.isEmpty, message = s"Entrypoints are not (yet) supported for resources of type ${`type`}.")
  
  val companion = RScript
  def copyResource(path: Option[String], text: Option[String], dest: Option[String], is_executable: Option[Boolean], parent: Option[URI]): Resource = {
    copy(path = path, text = text, dest = dest, is_executable = is_executable, parent = parent)
  }

  def generateInjectionMods(functionality: Functionality): ScriptInjectionMods = {
    val argsAndMeta = functionality.getArgumentLikesGroupedByDest(includeMeta = true, filterInputs = true)

    val paramsCode = argsAndMeta.map { case (dest, params) =>

      val parSet = params.map { par =>
        // val env_name = par.VIASH_PAR
        val env_name = Bash.getEscapedArgument(par.VIASH_PAR, "paste0(r'(", ")')", """\'""", """)\', \"\'\", r\'(""")

        val parse = par match {
          case a: BooleanArgumentBase if a.multiple =>
          s"""as.logical(strsplit(toupper($env_name), split = '${a.multiple_sep}')[[1]])"""
        case a: IntegerArgument if a.multiple =>
          s"""as.integer(strsplit($env_name, split = '${a.multiple_sep}')[[1]])"""
        case a: LongArgument if a.multiple =>
          s"""bit64::as.integer64(strsplit($env_name, split = '${a.multiple_sep}')[[1]])"""
        case a: DoubleArgument if a.multiple =>
          s"""as.numeric(strsplit($env_name, split = '${a.multiple_sep}')[[1]])"""
        case a: FileArgument if a.multiple =>
          s"""strsplit($env_name, split = '${a.multiple_sep}')[[1]]"""
        case a: StringArgument if a.multiple =>
          s"""strsplit($env_name, split = '${a.multiple_sep}')[[1]]"""
        case _: BooleanArgumentBase => s"""as.logical(toupper($env_name))"""
        case _: IntegerArgument => s"""as.integer($env_name)"""
        case _: LongArgument => s"""bit64::as.integer64($env_name)"""
        case _: DoubleArgument => s"""as.numeric($env_name)"""
        case _: FileArgument => s"""$env_name"""
        case _: StringArgument => s"""$env_name"""
        }

        s""""${par.plainName}" = $$VIASH_DOLLAR$$( if [ ! -z $${${par.VIASH_PAR}+x} ]; then echo "$parse"; else echo NULL; fi )"""
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