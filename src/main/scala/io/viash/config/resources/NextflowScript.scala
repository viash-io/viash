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

package io.viash.config.resources

import io.viash.schemas._

import java.net.URI
import java.nio.file.Path
import java.nio.file.Paths
import io.viash.config.Config
import io.viash.config.arguments.Argument
import io.viash.runners.nextflow.NextflowHelper
import io.circe.syntax._
import io.viash.helpers.circe._
import io.viash.ViashNamespace
import io.viash.config.dependencies.Dependency
import io.viash.config.arguments._
import io.viash.helpers.Bash

@description("""A Nextflow script. Work in progress; added mainly for annotation at the moment.""".stripMargin)
@subclass("nextflow_script")
case class NextflowScript(
  path: Option[String] = None,
  text: Option[String] = None,
  dest: Option[String] = None,
  is_executable: Option[Boolean] = Some(true),
  parent: Option[URI] = None,

  @description("The name of the workflow to be wrapped.")
  entrypoint: String,

  @description("Specifies the resource as a Nextflow script.")
  `type`: String = NextflowScript.`type`
) extends Script {

  val companion = NextflowScript

  def copyResource(path: Option[String], text: Option[String], dest: Option[String], is_executable: Option[Boolean], parent: Option[URI]): Resource = {
    copy(path = path, text = text, dest = dest, is_executable = is_executable, parent = parent)
  }

def generateInjectionMods(argsMetaAndDeps: Map[String, List[Argument[_]]], config: Config): ScriptInjectionMods = {
    val paramsCode = argsMetaAndDeps.map { case (dest, params) =>
      val parSet = params.map { par =>
        // val env_name = par.VIASH_PAR
        val env_name = Bash.getEscapedArgument(par.VIASH_PAR, "'''", "'''", "'", """''' + ''''''' + '''""")

        val parse = par match {
          case a: BooleanArgumentBase if a.multiple =>
            s"""$env_name.split('${a.multiple_sep}').collect { it.toLowerCase() == 'true' }"""
          case a: IntegerArgument if a.multiple =>
            s"""$env_name.split('${a.multiple_sep}').collect { Integer.parseInt(it) }"""
          case a: LongArgument if a.multiple =>
            s"""$env_name.split('${a.multiple_sep}').collect { Long.parseLong(it) }"""
          case a: DoubleArgument if a.multiple =>
            s"""$env_name.split('${a.multiple_sep}').collect { Double.parseDouble(it) }"""
          case a: FileArgument if a.multiple =>
            s"""$env_name.split('${a.multiple_sep}')"""
          case a: StringArgument if a.multiple =>
            s"""$env_name.split('${a.multiple_sep}')"""
          case _: BooleanArgumentBase => s"""$env_name.toLowerCase() == 'true'"""
          case _: IntegerArgument => s"""Integer.parseInt($env_name)"""
          case _: LongArgument => s"""Long.parseLong($env_name)"""
          case _: DoubleArgument => s"""Double.parseDouble($env_name)"""
          case _: FileArgument => s"""$env_name"""
          case _: StringArgument => s"""$env_name"""
        }

        val notFound = "null"

        s"""'${par.plainName}': $$VIASH_DOLLAR$$( if [ ! -z $${${par.VIASH_PAR}+x} ]; then echo "$parse"; else echo $notFound; fi )"""
      }

      s"""def $dest = [
        |  ${parSet.mkString(",\n  ")}
        |]
        |""".stripMargin
    }

    val footer = s"""
      |workflow {
      |  Channel.fromList([
      |    ['run', par]
      |  ])
      |    | ${entrypoint}
      |}
      |""".stripMargin

    ScriptInjectionMods(
      params = paramsCode.mkString,
      footer = footer
    )
  }
}

object NextflowScript extends ScriptCompanion {
  val commentStr = "//"
  val extension = "nf"
  val `type` = "nextflow_script"
  val executor = Seq("nextflow", "run", ".", "-main-script")

}
