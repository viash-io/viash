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

case class ScalaScript(
  path: Option[String] = None,
  text: Option[String] = None,
  dest: Option[String] = None,
  is_executable: Option[Boolean] = Some(true),
  parent: Option[URI] = None,
  oType: String = "scala_script"
) extends Script {
  val meta = ScalaScript
  def copyResource(path: Option[String], text: Option[String], dest: Option[String], is_executable: Option[Boolean], parent: Option[URI]): Resource = {
    copy(path = path, text = text, dest = dest, is_executable = is_executable, parent = parent)
  }

  def generatePlaceholder(functionality: Functionality): String = {
    val params = functionality.arguments.filter(d => d.direction == Input || d.isInstanceOf[FileObject])

    val parClassTypes = params.map { par =>
      val classType = par match {
        case o: BooleanObject if o.multiple => "List[Boolean]"
        case o: IntegerObject if o.multiple => "List[Integer]"
        case o: DoubleObject if o.multiple => "List[Double]"
        case o: FileObject if o.multiple => "List[String]"
        case o: StringObject if o.multiple => "List[String]"
        // we could argue about whether these should be options or not
        case o: BooleanObject if !o.required && o.flagValue.isEmpty => "Option[Boolean]"
        case o: IntegerObject if !o.required => "Option[Integer]"
        case o: DoubleObject if !o.required => "Option[Double]"
        case o: FileObject if !o.required => "Option[String]"
        case o: StringObject if !o.required => "Option[String]"
        case _: BooleanObject => "Boolean"
        case _: IntegerObject => "Integer"
        case _: DoubleObject => "Double"
        case _: FileObject => "String"
        case _: StringObject => "String"
      }
      par.plainName + ": " + classType
    }
    val parSet = params.map { par =>
      // val env_name = par.VIASH_PAR
      val quo = "\"'\"'\""
      val env_name = par.viash_par_escaped(quo, """\"""", """\\\"""")

      val parse = { par match {
        case o: BooleanObject if o.multiple =>
          s"""$env_name.split($quo${o.multiple_sep}$quo).map(_.toLowerCase.toBoolean).toList"""
        case o: IntegerObject if o.multiple =>
          s"""$env_name.split($quo${o.multiple_sep}$quo).map(_.toInt).toList"""
        case o: DoubleObject if o.multiple =>
          s"""$env_name.split($quo${o.multiple_sep}$quo).map(_.toDouble).toList"""
        case o: FileObject if o.multiple =>
          s"""$env_name.split($quo${o.multiple_sep}$quo).toList"""
        case o: StringObject if o.multiple =>
          s"""$env_name.split($quo${o.multiple_sep}$quo).toList"""
        case o: BooleanObject if !o.required && o.flagValue.isEmpty => s"""Some($env_name.toLowerCase.toBoolean)"""
        case o: IntegerObject if !o.required => s"""Some($env_name.toInt)"""
        case o: DoubleObject if !o.required => s"""Some($env_name.toDouble)"""
        case o: FileObject if !o.required => s"""Some($env_name)"""
        case o: StringObject if !o.required => s"""Some($env_name)"""
        case _: BooleanObject => s"""$env_name.toLowerCase.toBoolean"""
        case _: IntegerObject => s"""$env_name.toInt"""
        case _: DoubleObject => s"""$env_name.toDouble"""
        case _: FileObject => s"""$env_name"""
        case _: StringObject => s"""$env_name"""
      }}

      val notFound = par match {
        case o: DataObject[_] if o.multiple => Some("Nil")
        case o: BooleanObject if o.flagValue.isDefined => None
        case o: DataObject[_] if !o.required => Some("None")
        case _: DataObject[_] => None
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
  val oType = "scala_script"

  def command(script: String): String = {
    "scala -nc \"" + script + "\""
  }

  def commandSeq(script: String): Seq[String] = {
    Seq("scala", "-nc", script)
  }
}