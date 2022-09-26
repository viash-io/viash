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
import io.viash.helpers.Bash

case class CSharpScript(
  path: Option[String] = None,
  text: Option[String] = None,
  dest: Option[String] = None,
  is_executable: Option[Boolean] = Some(true),
  parent: Option[URI] = None,
  entrypoint: Option[String] = None,
  `type`: String = CSharpScript.`type`
) extends Script {
  assert(entrypoint.isEmpty, message = s"Entrypoints are not (yet) supported for resources of type ${`type`}.")
  val companion = CSharpScript
  def copyResource(path: Option[String], text: Option[String], dest: Option[String], is_executable: Option[Boolean], parent: Option[URI]): Resource = {
    copy(path = path, text = text, dest = dest, is_executable = is_executable, parent = parent)
  }

  def generateInjectionMods(functionality: Functionality): ScriptInjectionMods = {
    val quo = "\"'\"'\""

    val params = functionality.allArguments.filter(d => d.direction == Input || d.isInstanceOf[FileArgument])

    val parSet = params.map { par =>
      // val env_name = par.VIASH_PAR
      val env_name = Bash.getEscapedArgument(par.VIASH_PAR, quo, """\"""", """\\\"""")

      val parse = { par match {
        case a: BooleanArgumentBase if a.multiple =>
          s"""$env_name.Split($quo${a.multiple_sep}$quo).Select(x => bool.Parse(x.ToLower())).ToArray()"""
        case a: IntegerArgument if a.multiple =>
          s"""$env_name.Split($quo${a.multiple_sep}$quo).Select(x => Convert.ToInt32(x)).ToArray()"""
        case a: LongArgument if a.multiple =>
          s"""$env_name.Split($quo${a.multiple_sep}$quo).Select(x => Convert.ToInt64(x)).ToArray()"""
        case a: DoubleArgument if a.multiple =>
          s"""$env_name.Split($quo${a.multiple_sep}$quo).Select(x => Convert.ToDouble(x)).ToArray()"""
        case a: FileArgument if a.multiple =>
          s"""$env_name.Split($quo${a.multiple_sep}$quo).ToArray()"""
        case a: StringArgument if a.multiple =>
          s"""$env_name.Split($quo${a.multiple_sep}$quo).ToArray()"""
        case _: BooleanArgumentBase => s"""bool.Parse($env_name.ToLower())"""
        case _: IntegerArgument => s"""Convert.ToInt32($env_name)"""
        case _: LongArgument => s"""Convert.ToInt64($env_name)"""
        case _: DoubleArgument => s"""Convert.ToDouble($env_name)"""
        case _: FileArgument => s"""$env_name"""
        case _: StringArgument => s"""$env_name"""
      }}

      val class_ = par match {
        case _: BooleanArgumentBase => "bool"
        case _: IntegerArgument => "int"
        case _: LongArgument => "long"
        case _: DoubleArgument => "double"
        case _: FileArgument => "string"
        case _: StringArgument => "string"
      }

      val notFound = par match {
        case a: Argument[_] if a.multiple => Some(s"new $class_[0]")
        case a: StringArgument if !a.required => Some(s"(${class_}) null")
        case a: FileArgument if !a.required => Some(s"(${class_}) null")
        case a: Argument[_] if !a.required => Some(s"(${class_}?) null")
        case _: Argument[_] => None
      }

      val setter = notFound match {
        case Some(nf) =>
          s"""$$VIASH_DOLLAR$$( if [ ! -z $${${par.VIASH_PAR}+x} ]; then echo "$parse"; else echo "$nf"; fi )"""
        case None => parse.replaceAll(quo, "\"")
      }

      s"${par.plainName} = $setter"
    }
    val metaSet = BashWrapper.metaFields.map{ case BashWrapper.ViashMeta(env_name, script_name, _) =>
      val env_name_escaped = Bash.getEscapedArgument(env_name, quo, """\"""", """\\\"""")
      s"""$script_name = $$VIASH_DOLLAR$$( if [ ! -z $${$env_name+x} ]; then echo "$env_name_escaped"; else echo "(string) null"; fi )"""
    }
    
    val paramsCode = 
      s"""var par = new {
       |  ${parSet.mkString(",\n  ")}
       |};
       |var meta = new {
       |  ${metaSet.mkString(",\n  ")}
       |};
       |var resources_dir = "$$VIASH_META_RESOURCES_DIR";
       |""".stripMargin

    ScriptInjectionMods(params = paramsCode)
  }

  def command(script: String): String = {
    "dotnet script \"" + script + "\""
  }

  def commandSeq(script: String): Seq[String] = {
    Seq("dotnet", "script", script)
  }
}

object CSharpScript extends ScriptCompanion {
  val commentStr = "//"
  val extension = "csx"
  val `type` = "csharp_script"
}

