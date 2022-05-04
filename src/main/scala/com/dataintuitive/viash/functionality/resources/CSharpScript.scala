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
import com.dataintuitive.viash.wrapper.BashWrapper

import java.net.URI

case class CSharpScript(
  path: Option[String] = None,
  text: Option[String] = None,
  dest: Option[String] = None,
  is_executable: Option[Boolean] = Some(true),
  parent: Option[URI] = None,
  `type`: String = "csharp_script"
) extends Script {
  val meta = CSharpScript
  def copyResource(path: Option[String], text: Option[String], dest: Option[String], is_executable: Option[Boolean], parent: Option[URI]): Resource = {
    copy(path = path, text = text, dest = dest, is_executable = is_executable, parent = parent)
  }

  def generatePlaceholder(functionality: Functionality): String = {
    val params = functionality.arguments.filter(d => d.direction == Input || d.isInstanceOf[FileObject])

    val parSet = params.map { par =>
      // val env_name = par.VIASH_PAR
      val quo = "\"'\"'\""
      val env_name = par.viash_par_escaped(quo, """\"""", """\\\"""")

      val parse = { par match {
        case o: BooleanObject if o.multiple =>
          s"""$env_name.Split($quo${o.multiple_sep}$quo).Select(x => bool.Parse(x.ToLower())).ToArray()"""
        case o: IntegerObject if o.multiple =>
          s"""$env_name.Split($quo${o.multiple_sep}$quo).Select(x => Convert.ToInt32(x)).ToArray()"""
        case o: DoubleObject if o.multiple =>
          s"""$env_name.Split($quo${o.multiple_sep}$quo).Select(x => Convert.ToDouble(x)).ToArray()"""
        case o: FileObject if o.multiple =>
          s"""$env_name.Split($quo${o.multiple_sep}$quo).ToArray()"""
        case o: StringObject if o.multiple =>
          s"""$env_name.Split($quo${o.multiple_sep}$quo).ToArray()"""
        case _: BooleanObject => s"""bool.Parse($env_name.ToLower())"""
        case _: IntegerObject => s"""Convert.ToInt32($env_name)"""
        case _: DoubleObject => s"""Convert.ToDouble($env_name)"""
        case _: FileObject => s"""$env_name"""
        case _: StringObject => s"""$env_name"""
      }}

      val class_ = par match {
        case _: BooleanObject => "bool"
        case _: IntegerObject => "int"
        case _: DoubleObject => "double"
        case _: FileObject => "string"
        case _: StringObject => "string"
      }

      val notFound = par match {
        case o: DataObject[_] if o.multiple => Some(s"new $class_[0]")
        case o: StringObject if !o.required => Some(s"(${class_}) null")
        case o: FileObject if !o.required => Some(s"(${class_}) null")
        case o: DataObject[_] if !o.required => Some(s"(${class_}?) null")
        case _: DataObject[_] => None
      }

      val setter = notFound match {
        case Some(nf) =>
          s"""$$VIASH_DOLLAR$$( if [ ! -z $${${par.VIASH_PAR}+x} ]; then echo "$parse"; else echo "$nf"; fi )"""
        case None => parse.replaceAll(quo, "\"")
      }

      s"${par.plainName} = $setter"
    }
    val metaSet = BashWrapper.metaFields.map{ case (env_name, script_name) =>
      s"""$script_name = "$$$env_name""""
    }
    s"""var par = new {
       |  ${parSet.mkString(",\n  ")}
       |};
       |var meta = new {
       |  ${metaSet.mkString(",\n  ")}
       |};
       |var resources_dir = "$$VIASH_META_RESOURCES_DIR";
       |""".stripMargin
  }
}

object CSharpScript extends ScriptObject {
  val commentStr = "//"
  val extension = "csx"
  val `type` = "csharp_script"

  def command(script: String): String = {
    "dotnet script \"" + script + "\""
  }

  def commandSeq(script: String): Seq[String] = {
    Seq("dotnet", "script", script)
  }
}

