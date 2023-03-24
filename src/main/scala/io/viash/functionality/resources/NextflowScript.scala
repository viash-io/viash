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
import io.viash.config.Config

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

  def generateInjectionMods(argsAndMeta: Map[String, List[Argument[_]]], config: Option[Config]): ScriptInjectionMods = {
    config match {
      case None =>
        ScriptInjectionMods()

      case Some(c) =>
        val configPath = s"$$targetDir/${c.functionality.namespace.getOrElse("namespace")}/${c.functionality.name}/.config.vsh.yaml"

        val dependenciesAndDirNames = c.functionality.dependencies.map(d => (d, d.repository.toOption.get.name))

        val dirStrings = dependenciesAndDirNames.map(_._2).distinct.map(name => s"""${name}Dir = params.rootDir + "/module_${name}/target/nextflow"""")       
        val depStrings = dependenciesAndDirNames.map{ case(dep, dir) => s"include { ${dep.workConfig.get.functionality.name} } from ${dir}Dir + '/${dep.name}/main.nf'" }

        val str = 
          s"""nextflow.enable.dsl=2
             |
             |// or include these in the file itself?
             |targetDir = params.rootDir + "/target/nextflow"
             |
             |include { readYaml; channelFromParams; preprocessInputs; helpMessage } from targetDir + "/helpers/WorkflowHelper.nf"
             |include { setWorkflowArguments; getWorkflowArguments } from targetDir + "/helpers/DataflowHelper.nf"
             |
             |config = readYaml("$configPath")
             |
             |// import dependencies
             |${dirStrings.mkString("\n|")}
             |
             |${depStrings.mkString("\n|")}
             |
             |workflow {
             |  helpMessage(config)
             |
             |  channelFromParams(params, config)
             |    | ${c.functionality.name}
             |    // todo: publish
             |}
             |
             |workflow ${c.functionality.name} {
             |  take:
             |  input_ch
             |
             |  main:
             |  output_ch = input_ch
             |    | preprocessInputs(config: config)
             |    | main
             |
             |  emit:
             |    output_ch
             |}
             |""".stripMargin
        ScriptInjectionMods(params = str)
    }

    
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