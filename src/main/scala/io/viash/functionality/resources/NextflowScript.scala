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
import java.nio.file.Paths
import io.viash.functionality.arguments.Argument
import io.viash.config.Config
import io.viash.platforms.nextflow.NextflowHelper
import io.viash.helpers.circe._
import io.circe.syntax._
import io.circe.{Printer => JsonPrinter}
import io.viash.ViashNamespace

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

  def generateInjectionMods(argsMetaAndDeps: Map[String, List[Argument[_]]], config: Config): ScriptInjectionMods = {
    val configPath = s"$$targetDir/${config.functionality.namespace.getOrElse("namespace")}/${config.functionality.name}/.config.vsh.yaml"

    val (localDependencies, remoteDependencies) = config.functionality.dependencies
      .partition(d => d.isLocalDependency)
    val localDependenciesStrings = localDependencies.map{ d =>
      // Go up the same amount of times that the namespace strategy specifies during the build step so we can go down to the local dependency folder
      val up = ViashNamespace.targetOutputPath("", "..", config.functionality.namespace.map(ns => ".."), "..")
      s"include { ${d.configInfo{"functionalityName"}} } from \"$$projectDir$up${d.configInfo.getOrElse("executable", "")}\""
    }
    val remoteDependenciesStrings = remoteDependencies.map{ d => 
      s"include { ${d.configInfo("functionalityName")} } from \"$$rootDir/dependencies/${d.subOutputPath.get}/main.nf\""
    }

    val jsonPrinter = JsonPrinter.spaces2.copy(dropNullValues = true)
    val funJson = config.asJson.dropEmptyRecursively
    val funJsonStr = jsonPrinter.print(funJson)
      .replace("\\\\", "\\\\\\\\")
      .replace("\\\"", "\\\\\"")
      .replace("'''", "\\'\\'\\'")
      .grouped(65000) // JVM has a maximum string limit of 65535
      .toList         // see https://stackoverflow.com/a/6856773
      .mkString("'''", "''' + '''", "'''")

    val str = 
      s"""nextflow.enable.dsl=2
          |
          |config = readJsonBlob($funJsonStr)
          |
          |// import dependencies
          |rootDir = getRootDir()
          |${localDependenciesStrings.mkString("\n|")}
          |${remoteDependenciesStrings.mkString("\n|")}
          |
          |workflow {
          |  helpMessage(config)
          |
          |  channelFromParams(params, config)
          |    | ${config.functionality.name}
          |    // todo: publish
          |}
          |
          |workflow ${config.functionality.name} {
          |  take:
          |  input_ch
          |
          |  main:
          |  output_ch = input_ch
          |    | preprocessInputs(config: config)
          |    | main_wf
          |
          |  emit:
          |    output_ch
          |}
          |""".stripMargin

    val footer = Seq("// END CUSTOM CODE", NextflowHelper.workflowHelper, NextflowHelper.dataflowHelper).mkString("\n\n", "\n\n", "")
    ScriptInjectionMods(params = str, footer = footer)
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