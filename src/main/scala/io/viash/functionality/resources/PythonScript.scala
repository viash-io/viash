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

import java.net.URI
import _root_.io.viash.helpers.Bash
import io.viash.schemas._
import io.viash.config.Config

@description("""An executable Python script.
               |When defined in functionality.resources, only the first entry will be executed when running the built component or when running `viash run`.
               |When defined in functionality.test_resources, all entries will be executed during `viash test`.""".stripMargin)
@subclass("python_script")
case class PythonScript(
  path: Option[String] = None,
  text: Option[String] = None,
  dest: Option[String] = None,
  is_executable: Option[Boolean] = Some(true),
  parent: Option[URI] = None,

  @description("Specifies the resource as a Python script.")
  `type`: String = PythonScript.`type`
) extends Script {
  val companion = PythonScript
  def copyResource(path: Option[String], text: Option[String], dest: Option[String], is_executable: Option[Boolean], parent: Option[URI]): Resource = {
    copy(path = path, text = text, dest = dest, is_executable = is_executable, parent = parent)
  }

  def generateInjectionMods(argsMetaAndDeps: Map[String, List[Argument[_]]], config: Config): ScriptInjectionMods = {
    val paramsCode = argsMetaAndDeps.map { case (dest, params) =>
      val parSet = params.map { par =>
      // val env_name = par.VIASH_PAR
      val env_name = Bash.getEscapedArgument(par.VIASH_PAR, "r'", "'", """\'""", """\'\"\'\"r\'""")

      val parse = par match {
        case a: BooleanArgumentBase if a.multiple =>
          s"""list(map(lambda x: (x.lower() == 'true'), $env_name.split('${a.multiple_sep}')))"""
        case a: IntegerArgument if a.multiple =>
          s"""list(map(int, $env_name.split('${a.multiple_sep}')))"""
        case a: LongArgument if a.multiple =>
          s"""list(map(int, $env_name.split('${a.multiple_sep}')))"""
        case a: DoubleArgument if a.multiple =>
          s"""list(map(float, $env_name.split('${a.multiple_sep}')))"""
        case a: FileArgument if a.multiple =>
          s"""$env_name.split('${a.multiple_sep}')"""
        case a: StringArgument if a.multiple =>
          s"""$env_name.split('${a.multiple_sep}')"""
        case _: BooleanArgumentBase => s"""$env_name.lower() == 'true'"""
        case _: IntegerArgument => s"""int($env_name)"""
        case _: LongArgument => s"""int($env_name)"""
        case _: DoubleArgument => s"""float($env_name)"""
        case _: FileArgument => s"""$env_name"""
        case _: StringArgument => s"""$env_name"""
      }

      val notFound = "None"

      s"""'${par.plainName}': $$VIASH_DOLLAR$$( if [ ! -z $${${par.VIASH_PAR}+x} ]; then echo "$parse"; else echo $notFound; fi )"""
    }

    s"""$dest = {
      |  ${parSet.mkString(",\n  ")}
      |}
      |""".stripMargin
    }

    ScriptInjectionMods(params = paramsCode.mkString)
  }
}

object PythonScript extends ScriptCompanion {
  val commentStr = "#"
  val extension = "py"
  val `type` = "python_script"
  // The -B argument stops Python from creating .pyc or .pyo files
  // on importing functions from other files.
  val executor = Seq("python", "-B")
}