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

case class ScalaScript(
  path: Option[String] = None,
  text: Option[String] = None,
  dest: Option[String] = None,
  is_executable: Option[Boolean] = Some(true),
  parent: Option[URI] = None,
  entrypoint: Option[String] = None,
  `type`: String = ScalaScript.`type`
) extends Script {
  assert(entrypoint.isEmpty, message = s"Entrypoints are not (yet) supported for resources of type ${`type`}.")
  
  val companion = ScalaScript
  def copyResource(path: Option[String], text: Option[String], dest: Option[String], is_executable: Option[Boolean], parent: Option[URI]): Resource = {
    copy(path = path, text = text, dest = dest, is_executable = is_executable, parent = parent)
  }

  def generateInjectionMods(functionality: Functionality): ScriptInjectionMods = {
    val params = functionality.allArguments.filter(d => d.direction == Input || d.isInstanceOf[FileArgument])

    val parClassTypes = params.map { par =>
      val classType = par match {
        case a: BooleanArgumentBase if a.multiple => "List[Boolean]"
        case a: IntegerArgument if a.multiple => "List[Integer]"
        case a: DoubleArgument if a.multiple => "List[Double]"
        case a: FileArgument if a.multiple => "List[String]"
        case a: StringArgument if a.multiple => "List[String]"
        // we could argue about whether these should be options or not
        case a: BooleanArgumentBase if !a.required && a.flagValue.isEmpty => "Option[Boolean]"
        case a: IntegerArgument if !a.required => "Option[Integer]"
        case a: DoubleArgument if !a.required => "Option[Double]"
        case a: FileArgument if !a.required => "Option[String]"
        case a: StringArgument if !a.required => "Option[String]"
        case _: BooleanArgumentBase => "Boolean"
        case _: IntegerArgument => "Integer"
        case _: DoubleArgument => "Double"
        case _: FileArgument => "String"
        case _: StringArgument => "String"
      }
      par.plainName + ": " + classType
    }
    val parSet = params.map { par =>
      // val env_name = par.VIASH_PAR
      val quo = "\"'\"'\""
      val env_name = Bash.getEscapedArgument(par.VIASH_PAR, quo, """\"""", """\\\"""")

      val parse = { par match {
        case a: BooleanArgumentBase if a.multiple =>
          s"""$env_name.split($quo${a.multiple_sep}$quo).map(_.toLowerCase.toBoolean).toList"""
        case a: IntegerArgument if a.multiple =>
          s"""$env_name.split($quo${a.multiple_sep}$quo).map(_.toInt).toList"""
        case a: DoubleArgument if a.multiple =>
          s"""$env_name.split($quo${a.multiple_sep}$quo).map(_.toDouble).toList"""
        case a: FileArgument if a.multiple =>
          s"""$env_name.split($quo${a.multiple_sep}$quo).toList"""
        case a: StringArgument if a.multiple =>
          s"""$env_name.split($quo${a.multiple_sep}$quo).toList"""
        case a: BooleanArgumentBase if !a.required && a.flagValue.isEmpty => s"""Some($env_name.toLowerCase.toBoolean)"""
        case a: IntegerArgument if !a.required => s"""Some($env_name.toInt)"""
        case a: DoubleArgument if !a.required => s"""Some($env_name.toDouble)"""
        case a: FileArgument if !a.required => s"""Some($env_name)"""
        case a: StringArgument if !a.required => s"""Some($env_name)"""
        case _: BooleanArgumentBase => s"""$env_name.toLowerCase.toBoolean"""
        case _: IntegerArgument => s"""$env_name.toInt"""
        case _: DoubleArgument => s"""$env_name.toDouble"""
        case _: FileArgument => s"""$env_name"""
        case _: StringArgument => s"""$env_name"""
      }}

      val notFound = par match {
        case a: Argument[_] if a.multiple => Some("Nil")
        case a: BooleanArgumentBase if a.flagValue.isDefined => None
        case a: Argument[_] if !a.required => Some("None")
        case _: Argument[_] => None
      }

      notFound match {
        case Some(nf) =>
          s"""$$VIASH_DOLLAR$$( if [ ! -z $${${par.VIASH_PAR}+x} ]; then echo "$parse"; else echo "$nf"; fi )"""
        case None => 
          parse.replaceAll(quo, "\"") // undo quote escape as string is not part of echo
      }
    }

    val metaClassTypes = BashWrapper.metaFields.map { case (_, script_name) =>
      script_name + ": String"
    }
    val metaSet = BashWrapper.metaFields.map { case (env_name, script_name) =>
      s""""$$$env_name""""
    }
    val paramsCode = s"""case class ViashPar(
       |  ${parClassTypes.mkString(",\n  ")}
       |)
       |val par = ViashPar(
       |  ${parSet.mkString(",\n  ")}
       |)
       |
       |case class ViashMeta(
       |  ${metaClassTypes.mkString(",\n  ")}
       |)
       |val meta = ViashMeta(
       |  ${metaSet.mkString(",\n  ")}
       |)
       |
       |val resources_dir = "$$VIASH_META_RESOURCES_DIR"
       |""".stripMargin
    ScriptInjectionMods(params = paramsCode)
  }

  def command(script: String): String = {
    "scala -nc \"" + script + "\""
  }

  def commandSeq(script: String): Seq[String] = {
    Seq("scala", "-nc", script)
  }
}

object ScalaScript extends ScriptCompanion {
  val commentStr = "//"
  val extension = "scala"
  val `type` = "scala_script"
}