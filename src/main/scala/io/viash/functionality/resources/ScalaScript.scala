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

@description("""An executable Scala script.
               |When defined in functionality.resources, only the first entry will be executed when running the built component or when running `viash run`.
               |When defined in functionality.test_resources, all entries will be executed during `viash test`.""".stripMargin)
case class ScalaScript(
  path: Option[String] = None,
  text: Option[String] = None,
  dest: Option[String] = None,
  is_executable: Option[Boolean] = Some(true),
  parent: Option[URI] = None,

  @description("Specifies the resource as a Scala script.")
  `type`: String = ScalaScript.`type`
) extends Script {
  val companion = ScalaScript
  def copyResource(path: Option[String], text: Option[String], dest: Option[String], is_executable: Option[Boolean], parent: Option[URI]): Resource = {
    copy(path = path, text = text, dest = dest, is_executable = is_executable, parent = parent)
  }

  def generateInjectionMods(argsAndMeta: Map[String, List[Argument[_]]]): ScriptInjectionMods = {
    val quo = "\"'\"\"\"'\""
    val paramsCode = argsAndMeta.map { case (dest, params) =>
      val parClassTypes = params.map { par =>
        val classType = par match {
          case a: BooleanArgumentBase if a.multiple => "List[Boolean]"
          case a: IntegerArgument if a.multiple => "List[Int]"
          case a: LongArgument if a.multiple => "List[Long]"
          case a: DoubleArgument if a.multiple => "List[Double]"
          case a: FileArgument if a.multiple => "List[String]"
          case a: StringArgument if a.multiple => "List[String]"
          // we could argue about whether these should be options or not
          case a: BooleanArgumentBase if !a.required && a.flagValue.isEmpty => "Option[Boolean]"
          case a: IntegerArgument if !a.required => "Option[Int]"
          case a: LongArgument if !a.required => "Option[Long]"
          case a: DoubleArgument if !a.required => "Option[Double]"
          case a: FileArgument if !a.required => "Option[String]"
          case a: StringArgument if !a.required => "Option[String]"
          case _: BooleanArgumentBase => "Boolean"
          case _: IntegerArgument => "Int"
          case _: LongArgument => "Long"
          case _: DoubleArgument => "Double"
          case _: FileArgument => "String"
          case _: StringArgument => "String"
          }
          par.plainName + ": " + classType
        }
        val parSet = params.map { par =>
          // val env_name = par.VIASH_PAR
          val env_name = Bash.getEscapedArgument(par.VIASH_PAR, quo, """\"""", """\"\"\"+\"\\\"\"+\"\"\"""")

        val parse = { par match {
          case a: BooleanArgumentBase if a.multiple =>
            s"""$env_name.split($quo${a.multiple_sep}$quo).map(_.toLowerCase.toBoolean).toList"""
          case a: IntegerArgument if a.multiple =>
            s"""$env_name.split($quo${a.multiple_sep}$quo).map(_.toInt).toList"""
          case a: LongArgument if a.multiple =>
            s"""$env_name.split($quo${a.multiple_sep}$quo).map(_.toLong).toList"""
          case a: DoubleArgument if a.multiple =>
            s"""$env_name.split($quo${a.multiple_sep}$quo).map(_.toDouble).toList"""
          case a: FileArgument if a.multiple =>
            s"""$env_name.split($quo${a.multiple_sep}$quo).toList"""
          case a: StringArgument if a.multiple =>
            s"""$env_name.split($quo${a.multiple_sep}$quo).toList"""
          case a: BooleanArgumentBase if !a.required && a.flagValue.isEmpty => s"""Some($env_name.toLowerCase.toBoolean)"""
          case a: IntegerArgument if !a.required => s"""Some($env_name.toInt)"""
          case a: LongArgument if !a.required => s"""Some($env_name.toLong)"""
          case a: DoubleArgument if !a.required => s"""Some($env_name.toDouble)"""
          case a: FileArgument if !a.required => s"""Some($env_name)"""
          case a: StringArgument if !a.required => s"""Some($env_name)"""
          case _: BooleanArgumentBase => s"""$env_name.toLowerCase.toBoolean"""
          case _: IntegerArgument => s"""$env_name.toInt"""
          case _: LongArgument => s"""$env_name.toLong"""
          case _: DoubleArgument => s"""$env_name.toDouble"""
          case _: FileArgument => s"""$env_name"""
          case _: StringArgument => s"""$env_name"""
        }}
        
        // Todo: set as None if multiple is undefined
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
            parse.replaceAll(quo, "\"\"\"") // undo quote escape as string is not part of echo
        }
      }

      s"""case class Viash${dest.capitalize}(
        |  ${parClassTypes.mkString(",\n  ")}
        |)
        |val $dest = Viash${dest.capitalize}(
        |  ${parSet.mkString(",\n  ")}
        |)
        |""".stripMargin
    }

    ScriptInjectionMods(params = paramsCode.mkString)
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