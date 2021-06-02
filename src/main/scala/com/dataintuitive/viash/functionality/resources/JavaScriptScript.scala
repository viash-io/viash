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
import com.dataintuitive.viash.functionality.dataobjects._

import java.net.URI

case class JavaScriptScript(
  path: Option[String] = None,
  text: Option[String] = None,
  dest: Option[String] = None,
  is_executable: Option[Boolean] = Some(true),
  parent: Option[URI] = None,
  oType: String = "javascript_script"
) extends Script {
  val meta = JavaScriptScript
  def copyResource(path: Option[String], text: Option[String], dest: Option[String], is_executable: Option[Boolean], parent: Option[URI]): Resource = {
    copy(path = path, text = text, dest = dest, is_executable = is_executable, parent = parent)
  }

  def generatePlaceholder(functionality: Functionality): String = {
    val params = functionality.arguments.filter(d => d.direction == Input || d.isInstanceOf[FileObject])

    val par_set = params.map { par =>
      val env_name = par.VIASH_PAR

      val parse = par match {
        case o: BooleanObject if o.multiple =>
          s"""'$$$env_name'.split('${o.multiple_sep}').map(x => x.toLowerCase() === 'true')"""
        case o: IntegerObject if o.multiple =>
          s"""'$$$env_name'.split('${o.multiple_sep}').map(x => parseInt(x))"""
        case o: DoubleObject if o.multiple =>
          s"""'$$$env_name'.split('${o.multiple_sep}').map(x => parseFloat(x))"""
        case o: FileObject if o.multiple =>
          s"""'$$$env_name'.split('${o.multiple_sep}')"""
        case o: StringObject if o.multiple =>
          s"""'$$$env_name'.split('${o.multiple_sep}')"""
        case _: BooleanObject => s"""'$$$env_name'.toLowerCase() === 'true'"""
        case _: IntegerObject => s"""parseInt('$$$env_name')"""
        case _: DoubleObject => s"""parseFloat('$$$env_name')"""
        case _: FileObject => s"""'$$$env_name'"""
        case _: StringObject => s"""'$$$env_name'"""
      }

      s"""'${par.plainName}': $$VIASH_DOLLAR$$( if [ ! -z $${$env_name+x} ]; then echo "$parse"; else echo undefined; fi )"""
    }
    s"""let par = {
       |  ${par_set.mkString(",\n  ")}
       |};
       |
       |let resources_dir = '$$VIASH_RESOURCES_DIR'
       |""".stripMargin
  }
}

object JavaScriptScript extends ScriptObject {
  val commentStr = "//"
  val extension = "js"
  val oType = "javascript_script"

  def command(script: String): String = {
    "node \"" + script + "\""
  }

  def commandSeq(script: String): Seq[String] = {
    Seq("node", script)
  }
}
