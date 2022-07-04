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

case class BashScript(
  path: Option[String] = None,
  text: Option[String] = None,
  dest: Option[String] = None,
  is_executable: Option[Boolean] = Some(true),
  parent: Option[URI] = None,
  entrypoint: Option[String] = None,
  `type`: String = BashScript.`type`
) extends Script {
  assert(entrypoint.isEmpty, message = s"Entrypoints are not (yet) supported for resources of type ${`type`}.")

  val companion = BashScript
  def copyResource(path: Option[String], text: Option[String], dest: Option[String], is_executable: Option[Boolean], parent: Option[URI]): Resource = {
    copy(path = path, text = text, dest = dest, is_executable = is_executable, parent = parent)
  }

  def generateInjectionMods(functionality: Functionality): ScriptInjectionMods = {
    val params = functionality.allArguments.filter(d => d.direction == Input || d.isInstanceOf[FileArgument])

    val parSet = params.map { par =>
      val parse = par.par + "=" + par.viash_par_escaped("'", """\'""", """\'\"\'\"\'""")
      s"""$$VIASH_DOLLAR$$( if [ ! -z $${${par.VIASH_PAR}+x} ]; then echo "$parse"; fi )"""
    }
    val metaSet = BashWrapper.metaFields.map{ case (env_name, script_name) =>
      s"""meta_$script_name='$$$env_name'""".stripMargin
    }
    val paramsCode = 
      s"""${parSet.mkString("\n")}
       |${metaSet.mkString("\n")}
       |resources_dir="$$VIASH_META_RESOURCES_DIR"
       |""".stripMargin
    ScriptInjectionMods(params = paramsCode)
  }

  def command(script: String): String = {
    "bash \"" + script + "\""
  }

  def commandSeq(script: String): Seq[String] = {
    Seq("bash", script)
  }
}

object BashScript extends ScriptCompanion {
  val commentStr = "#"
  val extension = "sh"
  val `type` = "bash_script"
}
