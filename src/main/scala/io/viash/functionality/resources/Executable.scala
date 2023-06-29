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

import java.net.URI
import java.nio.file.Path
import io.viash.functionality.arguments.Argument
import io.viash.schemas._

@description("An executable file.")
@subclass("executable")
case class Executable(
  path: Option[String] = None,
  text: Option[String] = None,
  dest: Option[String] = None,
  is_executable: Option[Boolean] = Some(true),
  parent: Option[URI] = None,

  @description("Specifies the resource as an executable.")
  `type`: String = "executable"
) extends Script {
  val companion = Executable
  def copyResource(path: Option[String], text: Option[String], dest: Option[String], is_executable: Option[Boolean], parent: Option[URI]): Resource = {
    copy(path = path, text = text, dest = dest, is_executable = is_executable, parent = parent)
  }

  def generateInjectionMods(argsAndMeta: Map[String, List[Argument[_]]]): ScriptInjectionMods = ScriptInjectionMods()

  override def read: Option[String] = None

  override def write(path: Path, overwrite: Boolean): Unit = {}

  def command(script: String): String = {
    script
  }

  def commandSeq(script: String): Seq[String] = {
    Seq(script)
  }
}

object Executable extends ScriptCompanion {
  val commentStr = "#"
  val extension = "*"
  val `type` = "executable"
}
