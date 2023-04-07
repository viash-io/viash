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
import io.viash.schemas._

import java.net.URI
import io.viash.functionality.arguments.Argument

@description("""A Nextflow script. Work in progress; added mainly for annotation at the moment.""".stripMargin)
case class NextflowScript(
  path: Option[String] = None,
  text: Option[String] = None,
  dest: Option[String] = None,
  is_executable: Option[Boolean] = Some(true),
  parent: Option[URI] = None,

  @description("The name of the workflow to be executed.")
  entrypoint: Option[String] = None,

  @description("Specifies the resource as a Nextflow script.")
  `type`: String = NextflowScript.`type`
) extends Script {
  
  val companion = NextflowScript

  def copyResource(path: Option[String], text: Option[String], dest: Option[String], is_executable: Option[Boolean], parent: Option[URI]): Resource = {
    copy(path = path, text = text, dest = dest, is_executable = is_executable, parent = parent)
  }

  def generateInjectionMods(argsAndMeta: Map[String, List[Argument[_]]]): ScriptInjectionMods = {
    ScriptInjectionMods()
  }

  def command(script: String): String = {
    val entryStr = entrypoint match {
      case Some(entry) => " -entry " + entry
      case None => ""
    }
    "nextflow run . -main-script \"" + script + "\"" + entryStr
  }

  def commandSeq(script: String): Seq[String] = {
    val entrySeq = entrypoint match {
      case Some(entry) => Seq("-entry", entry)
      case None => Seq()
    }
    // Seq("nextflow", "run", script) ++ entrySeq
    Seq("nextflow", "run", ".", "-main-script", script) ++ entrySeq
  }
}

object NextflowScript extends ScriptCompanion {
  val commentStr = "//"
  val extension = "nf"
  val `type` = "nextflow_script"
}