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
import io.viash.functionality.arguments._
import io.viash.wrapper.BashWrapper
import io.viash.schemas._

import java.net.URI
import io.viash.helpers.Bash

@description("""An executable JavaScript script.
               |When defined in functionality.resources, only the first entry will be executed when running the built component or when running `viash run`.
               |When defined in functionality.test_resources, all entries will be executed during `viash test`.""".stripMargin)
case class JavaScriptScript(
  path: Option[String] = None,
  text: Option[String] = None,
  dest: Option[String] = None,
  is_executable: Option[Boolean] = Some(true),
  parent: Option[URI] = None,

  @description("Specifies the resource as a JavaScript script.")
  `type`: String = JavaScriptScript.`type`
) extends Script {
  val companion = JavaScriptScript
  def copyResource(path: Option[String], text: Option[String], dest: Option[String], is_executable: Option[Boolean], parent: Option[URI]): Resource = {
    copy(path = path, text = text, dest = dest, is_executable = is_executable, parent = parent)
  }

  def generateInjectionMods(argsAndMeta: Map[String, List[Argument[_]]]): ScriptInjectionMods = {
    val paramsCode = argsAndMeta.map { case (dest, params) =>
    val parSet = params.map { par =>
      // val env_name = par.VIASH_PAR
      val env_name = Bash.getEscapedArgument(par.VIASH_PAR, "String.raw`", "`", """`""", """`+\"`\"+String.raw`""")

      val parse = par match {
        case a: BooleanArgumentBase if a.multiple =>
          s"""$env_name.split('${a.multiple_sep}').map(x => x.toLowerCase() === 'true')"""
        case a: IntegerArgument if a.multiple =>
          s"""$env_name.split('${a.multiple_sep}').map(x => parseInt(x))"""
        case a: LongArgument if a.multiple =>
          s"""$env_name.split('${a.multiple_sep}').map(x => parseInt(x))"""
        case a: DoubleArgument if a.multiple =>
          s"""$env_name.split('${a.multiple_sep}').map(x => parseFloat(x))"""
        case a: FileArgument if a.multiple =>
          s"""$env_name.split('${a.multiple_sep}')"""
        case a: StringArgument if a.multiple =>
          s"""$env_name.split('${a.multiple_sep}')"""
        case _: BooleanArgumentBase => s"""$env_name.toLowerCase() === 'true'"""
        case _: IntegerArgument => s"""parseInt($env_name)"""
        case _: LongArgument => s"""parseInt($env_name)"""
        case _: DoubleArgument => s"""parseFloat($env_name)"""
        case _: FileArgument => s"""$env_name"""
        case _: StringArgument => s"""$env_name"""
      }

      val notFound = "undefined"

      s"""'${par.plainName}': $$VIASH_DOLLAR$$( if [ ! -z $${${par.VIASH_PAR}+x} ]; then echo "$parse"; else echo $notFound; fi )"""
    }
    s"""let $dest = {
      |  ${parSet.mkString(",\n  ")}
      |};
      |""".stripMargin
    }
    ScriptInjectionMods(params = paramsCode.mkString)
  }

  def command(script: String): String = {
    "node \"" + script + "\""
  }

  def commandSeq(script: String): Seq[String] = {
    Seq("node", script)
  }
}

object JavaScriptScript extends ScriptCompanion {
  val commentStr = "//"
  val extension = "js"
  val `type` = "javascript_script"
}
