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

case class ScalaScript(
  path: Option[String] = None,
  text: Option[String] = None,
  dest: Option[String] = None,
  is_executable: Option[Boolean] = Some(true),
  parent: Option[URI] = None,
  `type`: String = "scala_script"
) extends Script {
  val meta = ScalaScript
  def copyResource(path: Option[String], text: Option[String], dest: Option[String], is_executable: Option[Boolean], parent: Option[URI]): Resource = {
    copy(path = path, text = text, dest = dest, is_executable = is_executable, parent = parent)
  }

  def generatePlaceholder(functionality: Functionality): String = {
    val params = functionality.allArguments.filter(d => d.direction == Input || d.isInstanceOf[FileArgument])

    val parClassTypes = params.map { par =>
      val classType = par match {
        case a: BooleanArgument if a.multiple => "List[Boolean]"
        case a: IntegerArgument if a.multiple => "List[Integer]"
        case a: DoubleArgument if a.multiple => "List[Double]"
        case a: FileArgument if a.multiple => "List[String]"
        case a: StringArgument if a.multiple => "List[String]"
        // we could argue about whether these should be options or not
        case a: BooleanArgument if !a.required && a.flagValue.isEmpty => "Option[Boolean]"
        case a: IntegerArgument if !a.required => "Option[Integer]"
        case a: DoubleArgument if !a.required => "Option[Double]"
        case a: FileArgument if !a.required => "Option[String]"
        case a: StringArgument if !a.required => "Option[String]"
        case _: BooleanArgument => "Boolean"
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
      val env_name = par.viash_par_escaped(quo, """\"""", """\\\"""")

      val parse = { par match {
        case a: BooleanArgument if a.multiple =>
          s"""$env_name.split($quo${a.multiple_sep}$quo).map(_.toLowerCase.toBoolean).toList"""
        case a: IntegerArgument if a.multiple =>
          s"""$env_name.split($quo${a.multiple_sep}$quo).map(_.toInt).toList"""
        case a: DoubleArgument if a.multiple =>
          s"""$env_name.split($quo${a.multiple_sep}$quo).map(_.toDouble).toList"""
        case a: FileArgument if a.multiple =>
          s"""$env_name.split($quo${a.multiple_sep}$quo).toList"""
        case a: StringArgument if a.multiple =>
          s"""$env_name.split($quo${a.multiple_sep}$quo).toList"""
        case a: BooleanArgument if !a.required && a.flagValue.isEmpty => s"""Some($env_name.toLowerCase.toBoolean)"""
        case a: IntegerArgument if !a.required => s"""Some($env_name.toInt)"""
        case a: DoubleArgument if !a.required => s"""Some($env_name.toDouble)"""
        case a: FileArgument if !a.required => s"""Some($env_name)"""
        case a: StringArgument if !a.required => s"""Some($env_name)"""
        case _: BooleanArgument => s"""$env_name.toLowerCase.toBoolean"""
        case _: IntegerArgument => s"""$env_name.toInt"""
        case _: DoubleArgument => s"""$env_name.toDouble"""
        case _: FileArgument => s"""$env_name"""
        case _: StringArgument => s"""$env_name"""
      }}

      val notFound = par match {
        case a: Argument[_] if a.multiple => Some("Nil")
        case a: BooleanArgument if a.flagValue.isDefined => None
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
    s"""case class ViashPar(
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
  }
}

object ScalaScript extends ScriptObject {
  val commentStr = "//"
  val extension = "scala"
  val `type` = "scala_script"

  def command(script: String): String = {
    "scala -nc \"" + script + "\""
  }

  def commandSeq(script: String): Seq[String] = {
    Seq("scala", "-nc", script)
  }
}