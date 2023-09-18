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
import java.nio.file.Path
import java.nio.file.Paths
import io.viash.config.Config
import io.viash.functionality.arguments.Argument
import io.viash.runners.nextflow.NextflowHelper
import io.circe.syntax._
import io.viash.helpers.circe._
import io.viash.ViashNamespace
import io.viash.functionality.dependencies.Dependency

@description("""A Nextflow script. Work in progress; added mainly for annotation at the moment.""".stripMargin)
@subclass("nextflow_script")
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
  
  assert(entrypoint.isDefined, "In a Nextflow script, the 'entrypoint' argument needs to be specified.")

  val companion = NextflowScript

  def copyResource(path: Option[String], text: Option[String], dest: Option[String], is_executable: Option[Boolean], parent: Option[URI]): Resource = {
    copy(path = path, text = text, dest = dest, is_executable = is_executable, parent = parent)
  }

  def generateInjectionMods(argsMetaAndDeps: Map[String, List[Argument[_]]], config: Config): ScriptInjectionMods = {
    // TODO ideally we'd already have 'thisPath' precalculated but until that day, calculate it here
    val thisPath = Paths.get(ViashNamespace.targetOutputPath("", "invalid_platform_name", config.functionality.namespace, config.functionality.name))

    val depStrs = config.functionality.dependencies.map(NextflowScript.renderInclude(_, thisPath))

    val configJson = config.asJson.dropEmptyRecursively
    val configJsonStr = configJson
      .toFormattedString("json")
      .replace("\\\\", "\\\\\\\\")
      .replace("\\\"", "\\\\\"")
      .replace("'''", "\\'\\'\\'")
      .grouped(65000) // JVM has a maximum string limit of 65535
      .toList         // see https://stackoverflow.com/a/6856773
      .mkString("'''", "''' + '''", "'''")

    val str = 
      s"""nextflow.enable.dsl=2
        |
        |config = processConfig(readJsonBlob($configJsonStr))
        |
        |// import dependencies
        |rootDir = getRootDir()
        |${depStrs.mkString("\n|")}
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
        |    | ${entrypoint.get}
        |
        |  emit:
        |    output_ch
        |}
        |""".stripMargin

    val footer = Seq("// END CUSTOM CODE", NextflowHelper.workflowHelper, NextflowHelper.dataflowHelper).mkString("\n\n", "\n\n", "")
    ScriptInjectionMods(params = str, footer = footer)
  }

  override def command(script: String): String = {
    val entryStr = entrypoint match {
      case Some(entry) => " -entry " + entry
      case None => ""
    }
    super.command(script) + entryStr
  }

  override def commandSeq(script: String): Seq[String] = {
    val entrySeq = entrypoint match {
      case Some(entry) => Seq("-entry", entry)
      case None => Seq()
    }
    super.commandSeq(script) ++ entrySeq
  }
}

object NextflowScript extends ScriptCompanion {
  val commentStr = "//"
  val extension = "nf"
  val `type` = "nextflow_script"
  val executor = Seq("nextflow", "run", ".", "-main-script")

  /**
    * Renders the include statement for a dependency.
    *
    * @param dependency The dependency to render
    * @param parentPath The path of the current output folder
    * @return The include statement for the dependency. Expected format:
    * 
    *   - For local dependencies (i.e. the dependency's source code is defined in the same project as the current repository):
    *     ```
    *     include { my_dep as my_alias } from "$projectDir/../../../target/nextflow/my_namespace/my_dep/main.nf"
    *     ```
    *   - For remote dependencies (i.e. the dependency is fetched from a different project -- either a local folder or a remote repository):
    *     ```
    *     include { my_dep as my_alias } from "$rootDir/dependencies/my_namespace/my_dep/main.nf"
    *     ```
    */
  def renderInclude(dependency: Dependency, parentPath: Path): String = {
    if (dependency.subOutputPath.isEmpty) {
      return s"// dependency '${dependency.name}' not found!"
    }

    val depName = dependency.configInfo("functionalityName")
    val aliasStr = dependency.alias.map(" as " + _).getOrElse("")

    val source =
      if (dependency.isLocalDependency) {
        val dependencyPath = Paths.get(dependency.configInfo.getOrElse("executable", ""))
        // can we use suboutputpath here?
        //val dependencyPath = Paths.get(dependency.subOutputPath.get)
        val relativePath = parentPath.relativize(dependencyPath)
        s"\"$$projectDir/$relativePath\""
      } else {
        s"\"$$rootDir/dependencies/${dependency.subOutputPath.get}/main.nf\""
      }

    s"include { $depName$aliasStr } from ${source}"
  }
}