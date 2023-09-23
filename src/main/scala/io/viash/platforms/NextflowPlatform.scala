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

package io.viash.platforms

import io.viash.config.Config
import io.viash.functionality._
import io.viash.functionality.resources._
import io.viash.functionality.arguments._
import io.viash.helpers.{Docker, Bash, DockerImageInfo, Helper}
import io.viash.helpers.circe._
import io.viash.platforms.nextflow._
import io.circe.syntax._
import io.circe.{Printer => JsonPrinter, Json, JsonObject}
import shapeless.syntax.singleton
import io.viash.schemas._
import io.viash.helpers.Escaper
import java.nio.file.Paths
import io.viash.ViashNamespace

/**
 * A Platform class for generating Nextflow (DSL2) modules.
 */
@description("""Platform for generating Nextflow VDSL3 modules.""".stripMargin)
// todo: add link to guide
@example(
  """platforms:
    |  - type: nextflow
    |    directives:
    |      label: [lowcpu, midmem]
    |""".stripMargin,
  "yaml")
@subclass("nextflow")
case class NextflowPlatform(
  @description("Every platform can be given a specific id that can later be referred to explicitly when running or building the Viash component.")
  @example("id: foo", "yaml")
  @default("nextflow")
  id: String = "nextflow",

  `type`: String = "nextflow",
  
  // nxf params
  @description(
    """@[Directives](nextflow_directives) are optional settings that affect the execution of the process. These mostly match up with the Nextflow counterparts.  
      |""".stripMargin)
  @example(
    """directives:
      |  container: rocker/r-ver:4.1
      |  label: highcpu
      |  cpus: 4
      |  memory: 16 GB""".stripMargin,
      "yaml")
  @default("Empty")
  directives: NextflowDirectives = NextflowDirectives(),

  @description(
    """@[Automated processing flags](nextflow_auto) which can be toggled on or off:
      |
      || Flag | Description | Default |
      ||---|---------|----|
      || `simplifyInput` | If `true`, an input tuple only containing only a single File (e.g. `["foo", file("in.h5ad")]`) is automatically transformed to a map (i.e. `["foo", [ input: file("in.h5ad") ] ]`). | `true` |
      || `simplifyOutput` | If `true`, an output tuple containing a map with a File (e.g. `["foo", [ output: file("out.h5ad") ] ]`) is automatically transformed to a map (i.e. `["foo", file("out.h5ad")]`). | `true` |
      || `transcript` | If `true`, the module's transcripts from `work/` are automatically published to `params.transcriptDir`. If not defined, `params.publishDir + "/_transcripts"` will be used. Will throw an error if neither are defined. | `false` |
      || `publish` | If `true`, the module's outputs are automatically published to `params.publishDir`.  Will throw an error if `params.publishDir` is not defined. | `false` |
      |
      |""".stripMargin)
  @example(
    """auto:
      |  publish: true""".stripMargin,
      "yaml")
  @default(
    """simplifyInput: true
      |simplifyOutput: true
      |transcript: false
      |publish: false
      |""".stripMargin)
  auto: NextflowAuto = NextflowAuto(),

  @description("Allows tweaking how the @[Nextflow Config](nextflow_config) file is generated.")
  @since("Viash 0.7.4")
  @default("A series of default labels to specify memory and cpu constraints")
  config: NextflowConfig = NextflowConfig(),

  @description("Whether or not to print debug messages.")
  @default("False")
  debug: Boolean = false,

  // TODO: solve differently
  @description("Specifies the Docker platform id to be used to run Nextflow.")
  @default("docker")
  container: String = "docker"
) extends Platform {
  def escapeSingleQuotedString(txt: String): String = {
    Escaper(txt, slash = true, singleQuote = true, newline = true)
  }

  def modifyFunctionality(config: Config, testing: Boolean): Functionality = {
    val condir = containerDirective(config)

    // create main.nf file
    val mainFile = PlainFile(
      dest = Some("main.nf"),
      text = Some(renderMainNf(config, condir))
    )
    val nextflowConfigFile = PlainFile(
      dest = Some("nextflow.config"),
      text = Some(renderNextflowConfig(config.functionality, condir))
    )

    // remove main
    val otherResources = config.functionality.additionalResources

    config.functionality.copy(
      resources = mainFile :: nextflowConfigFile :: otherResources
    )
  }

  def containerDirective(config: Config): Option[DockerImageInfo] = {
    val plat = config.platforms.find(p => p.id == container)
    plat match {
      case Some(p: DockerPlatform) => 
        Some(Docker.getImageInfo(
          functionality = Some(config.functionality),
          registry = p.target_registry,
          organization = p.target_organization,
          name = p.target_image,
          tag = p.target_tag.map(_.toString),
          namespaceSeparator = p.namespace_separator
        ))
      case Some(_) => 
        throw new RuntimeException(s"NextflowPlatform 'container' variable: Platform $container is not a Docker Platform")
      case None => None
      case _ => ???
    }
  }

  def renderNextflowConfig(functionality: Functionality, containerDirective: Option[DockerImageInfo]): String = {
    val versStr = functionality.version.map(ver => s"\n  version = '$ver'").getOrElse("")

    val descStr = functionality.description.map{des => 
      val escDes = escapeSingleQuotedString(des)
      s"\n  description = '$escDes'"
    }.getOrElse("")

    val authStr = 
      if (functionality.authors.isEmpty) {
        "" 
      } else {
        val escAut = escapeSingleQuotedString(functionality.authors.map(_.name).mkString(", "))
        s"\n  author = '$escAut'"
      }

    // TODO: define profiles
    val profileStr = 
      if (containerDirective.isDefined || functionality.mainScript.map(_.`type`) == Some(NextflowScript.`type`)) {
        "\n\n" + NextflowHelper.profilesHelper
      } else {
        ""
      }

    val processLabels = config.labels.map{ case (k, v) => s"withLabel: $k { $v }"}
    val inlineScript = config.script.toList

    s"""manifest {
    |  name = '${functionality.name}'
    |  mainScript = 'main.nf'
    |  nextflowVersion = '!>=20.12.1-edge'$versStr$descStr$authStr
    |}$profileStr
    |
    |process{
    |  ${processLabels.mkString("\n  ")}
    |}
    |
    |${inlineScript.mkString("\n")}
    |""".stripMargin
  }

  // interpreted from BashWrapper
  def renderMainNf(config: Config, containerDirective: Option[DockerImageInfo]): String = {

    /************************* MAIN.NF *************************/
    config.functionality.mainScript match {
      // if mainResource is empty (shouldn't be the case)
      case None => throw new RuntimeException("there should be a main script here")

      // if mainResource is simply an executable
      case Some(e: Executable) => //" " + e.path.get + " $VIASH_EXECUTABLE_ARGS"
        throw new NotImplementedError(
          "Running executables through a NextflowPlatform is not (yet) implemented. " +
            "Create a support ticket to request this functionality if necessary."
        )
      
      case Some(res: NextflowScript) =>
        // TODO ideally we'd already have 'thisPath' precalculated but until that day, calculate it here
        val thisPath = Paths.get(ViashNamespace.targetOutputPath("", "invalid_platform_name", config.functionality.namespace, config.functionality.name))

        val depStrs = config.functionality.dependencies.map{ dep =>
          NextflowHelper.renderInclude(dep, thisPath)
        }

        s"""nextflow.enable.dsl=2
          |
          |// DEFINE CUSTOM CODE
          |
          |// component metadata
          |thisConfig = ${NextflowHelper.generateConfigStr(config)}
          |
          |// import dependencies
          |rootDir = getRootDir()
          |${depStrs.mkString("\n|")}
          |
          |// inner workflow hook
          |def innerWorkflowFactory(args) {
          |  return ${res.entrypoint.get}
          |}
          |
          |// component settings
          |thisDefaultProcessArgs = ${NextflowHelper.generateDefaultProcessArgs(config, directives, auto, debug)}
          |
          |// initialise default workflow
          |myWfInstance = workflowFactory([:])
          |
          |// add workflow to environment
          |nextflow.script.ScriptMeta.current().addDefinition(myWfInstance)
          |
          |workflow {
          |  helpMessage(thisConfig)
          |
          |  channelFromParams(params, thisConfig)
          |    | myWfInstance
          |    // todo: publish
          |}
          |
          |${res.readWithInjection(Map.empty, config).get.split("\n").mkString("\n|")}
          |
          |// END CUSTOM CODE
          |
          |////////////////////////////
          |// VDSL3 helper functions //
          |////////////////////////////
          |
          |""".stripMargin +
          NextflowHelper.workflowHelper

      // if mainResource is a script
      case Some(res) =>
        val directivesToJson = directives.copy(
          // if a docker platform is defined but the directives.container isn't, use the image of the dockerplatform as default
          container = directives.container orElse containerDirective.map(cd => Left(cd.toMap)),
          // is memory requirements are defined but directives.memory isn't, use that instead
          memory = directives.memory orElse config.functionality.requirements.memoryAsBytes.map(_.toString + " B"),
          // is cpu requirements are defined but directives.cpus isn't, use that instead
          cpus = directives.cpus orElse config.functionality.requirements.cpus.map(np => Left(np))
        )

        
        s"""${NextflowHelper.generateHeader(config)}
          |
          |nextflow.enable.dsl=2
          |
          |// DEFINE CUSTOM CODE
          |
          |// component metadata
          |thisConfig = ${NextflowHelper.generateConfigStr(config)}
          |
          |// process script
          |thisScript = ${NextflowHelper.generateScriptStr(config)}
          |
          |// inner workflow hook
          |def innerWorkflowFactory(args) {
          |  return vdsl3RunWorkflowFactory(args)
          |}
          |
          |// component settings
          |thisDefaultProcessArgs = ${NextflowHelper.generateDefaultProcessArgs(config, directivesToJson, auto, debug)}
          |
          |// retrieve resourcesDir here to make sure the correct path is found
          |resourcesDir = nextflow.script.ScriptMeta.current().getScriptPath().getParent()
          |
          |// initialise default workflow
          |myWfInstance = workflowFactory([:])
          |
          |// add workflow to environment
          |nextflow.script.ScriptMeta.current().addDefinition(myWfInstance)
          |
          |// anonymous workflow for running this module as a standalone
          |workflow {
          |  def mergedConfig = thisConfig
          |  def mergedParams = [:] + params
          |
          |  // add id argument if it's not already in the config
          |  if (mergedConfig.functionality.arguments.every{it.plainName != "id"}) {
          |    def idArg = [
          |      'name': '--id',
          |      'required': false,
          |      'type': 'string',
          |      'description': 'A unique id for every entry.',
          |      'multiple': false
          |    ]
          |    mergedConfig.functionality.arguments.add(0, idArg)
          |    mergedConfig = processConfig(mergedConfig)
          |  }
          |  if (!mergedParams.containsKey("id")) {
          |    mergedParams.id = "run"
          |  }
          |
          |  helpMessage(mergedConfig)
          |
          |  channelFromParams(mergedParams, mergedConfig)
          |    | preprocessInputs("config": mergedConfig)
          |    | myWfInstance.run(
          |      auto: [ publish: true ]
          |    ) // todo: allow publishStates publishing
          |}
          |
          |// END CUSTOM CODE
          |
          |////////////////////////////
          |// VDSL3 helper functions //
          |////////////////////////////
          |
          |""".stripMargin +
          NextflowHelper.workflowHelper
    }
  }
}

// vim: tabstop=2:softtabstop=2:shiftwidth=2:expandtab