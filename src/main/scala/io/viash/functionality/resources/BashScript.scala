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
import io.viash.config.Config

@description("""An executable Bash script.
               |When defined in functionality.resources, only the first entry will be executed when running the built component or when running `viash run`.
               |When defined in functionality.test_resources, all entries will be executed during `viash test`.""".stripMargin)
case class BashScript(
  path: Option[String] = None,
  text: Option[String] = None,
  dest: Option[String] = None,
  is_executable: Option[Boolean] = Some(true),
  parent: Option[URI] = None,

  @undocumented
  entrypoint: Option[String] = None,

  @description("Specifies the resource as a Bash script.")
  `type`: String = BashScript.`type`
) extends Script {
  assert(entrypoint.isEmpty, message = s"Entrypoints are not (yet) supported for resources of type ${`type`}.")

  val companion = BashScript
  def copyResource(path: Option[String], text: Option[String], dest: Option[String], is_executable: Option[Boolean], parent: Option[URI]): Resource = {
    copy(path = path, text = text, dest = dest, is_executable = is_executable, parent = parent)
  }

  def generateInjectionMods(argsAndMeta: Map[String, List[Argument[_]]], config: Config): ScriptInjectionMods = {
    val slash = "\\VIASH_SLASH\\"
    val parSet = argsAndMeta.values.flatten.map { par =>
      s"""$$VIASH_DOLLAR$$( if [ ! -z $${${par.VIASH_PAR}+x} ]; then echo "$${${par.VIASH_PAR}}" | sed "s#'#'$slash"'$slash"'#g;s#.*#${par.par}='&'#" ; else echo "# ${par.par}="; fi )"""
    }
    val dependencies = config.functionality.dependencies.map{ d =>
      s"""$$VIASH_DOLLAR$$( if [ ! -z $${${d.VIASH_DEP}+x} ]; then echo "$${${d.VIASH_DEP}}" | sed "s#'#'$slash"'$slash"'#g;s#.*#${d.scriptName}='&'#" ; else echo "# ${d.scriptName}="; fi )"""
    }

    val paramsCode = (parSet ++ dependencies).mkString("", "\n", "\n")
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
