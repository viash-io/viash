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

package com.dataintuitive.viash.functionality.resources

import com.dataintuitive.viash.functionality._
import com.dataintuitive.viash.functionality.arguments._
import com.dataintuitive.viash.wrapper.BashWrapper

import java.net.URI

case class PythonScript(
  path: Option[String] = None,
  text: Option[String] = None,
  dest: Option[String] = None,
  is_executable: Option[Boolean] = Some(true),
  parent: Option[URI] = None
) extends Script {
  val companion = PythonScript
  def copyResource(path: Option[String], text: Option[String], dest: Option[String], is_executable: Option[Boolean], parent: Option[URI]): Resource = {
    copy(path = path, text = text, dest = dest, is_executable = is_executable, parent = parent)
  }

  def generatePlaceholder(functionality: Functionality): String = {
    val params = functionality.allArguments.filter(d => d.direction == Input || d.isInstanceOf[FileArgument])

    val parSet = params.map { par =>
      // val env_name = par.VIASH_PAR
      val env_name = par.viash_par_escaped("'", """\'""", """\\\'""")

      val parse = par match {
        case a: BooleanArgument if a.multiple =>
          s"""list(map(lambda x: (x.lower() == 'true'), $env_name.split('${a.multiple_sep}')))"""
        case a: IntegerArgument if a.multiple =>
          s"""list(map(int, $env_name.split('${a.multiple_sep}')))"""
        case a: DoubleArgument if a.multiple =>
          s"""list(map(float, $env_name.split('${a.multiple_sep}')))"""
        case a: FileArgument if a.multiple =>
          s"""$env_name.split('${a.multiple_sep}')"""
        case a: StringArgument if a.multiple =>
          s"""$env_name.split('${a.multiple_sep}')"""
        case _: BooleanArgument => s"""$env_name.lower() == 'true'"""
        case _: IntegerArgument => s"""int($env_name)"""
        case _: DoubleArgument => s"""float($env_name)"""
        case _: FileArgument => s"""$env_name"""
        case _: StringArgument => s"""$env_name"""
      }

      s"""'${par.plainName}': $$VIASH_DOLLAR$$( if [ ! -z $${${par.VIASH_PAR}+x} ]; then echo "$parse"; else echo None; fi )"""
    }
    val metaSet = BashWrapper.metaFields.map{ case (env_name, script_name) =>
      s"""'$script_name': '$$$env_name'"""
    }

    s"""par = {
       |  ${parSet.mkString(",\n  ")}
       |}
       |meta = {
       |  ${metaSet.mkString(",\n  ")}
       |}
       |
       |resources_dir = '$$VIASH_META_RESOURCES_DIR'
       |""".stripMargin
  }
}

object PythonScript extends ScriptCompanion("python_script", "py", "#") {
  def command(script: String): String = {
    "python \"" + script + "\""
  }

  def commandSeq(script: String): Seq[String] = {
    Seq("python", script)
  }
}