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
      || `simplifyOutput` | If `true`, an output tuple containing a map with a File (e.g. `["foo", [ output: file("out.h5ad") ] ]`) is automatically transformed to a map (i.e. `["foo", file("out.h5ad")]`). | `false` |
      || `transcript` | If `true`, the module's transcripts from `work/` are automatically published to `params.transcriptDir`. If not defined, `params.publishDir + "/_transcripts"` will be used. Will throw an error if neither are defined. | `false` |
      || `publish` | If `true`, the module's outputs are automatically published to `params.publishDir`. If equal to `"state"`, also a `.state.yaml` file will be published in the publish dir. Will throw an error if `params.publishDir` is not defined. | `false` |
      |
      |""".stripMargin)
  @example(
    """auto:
      |  publish: true""".stripMargin,
      "yaml")
  @default(
    """simplifyInput: true
      |simplifyOutput: false
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
    // TODO: create and write dockerfile when #518 is merged into main
    // val dockerfile = PlainFile(
    //   dest = Some("Dockerfile"),
    //   text = Some(dockerEngine.dockerFile(...))
    // )

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

    val name = f"${functionality.namespace.map(_ + "/").getOrElse("")}${functionality.name}"

    s"""manifest {
    |  name = '${name}'
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
    if (config.functionality.mainScript.isEmpty) {
      throw new RuntimeException("No main script defined")
    }

    val mainScript = config.functionality.mainScript.get

    if (mainScript.isInstanceOf[Executable]) {
      throw new NotImplementedError(
        "Running executables through a NextflowPlatform is not (yet) implemented. " +
          "Create a support ticket to request this functionality if necessary."
      )
    }

    /************************* MAIN.NF *************************/

    val directivesToJson = directives.copy(
      // if a docker platform is defined but the directives.container isn't, use the image of the dockerplatform as default
      container = directives.container orElse containerDirective.map(cd => Left(cd.toMap)),
      // is memory requirements are defined but directives.memory isn't, use that instead
      memory = directives.memory orElse config.functionality.requirements.memoryAsBytes.map(_.toString + " B"),
      // is cpu requirements are defined but directives.cpus isn't, use that instead
      cpus = directives.cpus orElse config.functionality.requirements.cpus.map(np => Left(np))
    )

    val innerWorkflowFactory = mainScript match {
      // if mainscript is a nextflow workflow
      case scr: NextflowScript =>
        s"""// user-provided Nextflow code
          |${scr.readWithoutInjection.get.split("\n").mkString("\n|")}
          |
          |// inner workflow hook
          |def innerWorkflowFactory(args) {
          |  return ${scr.entrypoint}
          |}""".stripMargin
      // else if it is a vdsl3 module
      case _ => 
        s"""// inner workflow hook
          |def innerWorkflowFactory(args) {
          |  def rawScript = ${NextflowHelper.generateScriptStr(config)}
          |  
          |  return vdsl3WorkflowFactory(args, meta, rawScript)
          |}
          |
          |""".stripMargin + 
          NextflowHelper.vdsl3Helper
    }

    // TODO: might need to change resources_dir to just moduleDir
    NextflowHelper.generateHeader(config) + "\n\n" +
      NextflowHelper.workflowHelper +
      s"""
      |
      |nextflow.enable.dsl=2
      |
      |// START COMPONENT-SPECIFIC CODE
      |
      |// create meta object
      |meta = [
      |  "resources_dir": moduleDir.toRealPath().normalize(),
      |  "config": ${NextflowHelper.generateConfigStr(config)}
      |]
      |
      |// resolve dependencies dependencies (if any)
      |${NextflowHelper.renderDependencies(config).split("\n").mkString("\n|")}
      |
      |// inner workflow
      |${innerWorkflowFactory.split("\n").mkString("\n|")}
      |
      |// defaults
      |meta["defaults"] = ${NextflowHelper.generateDefaultWorkflowArgs(config, directivesToJson, auto, debug)}
      |
      |// initialise default workflow
      |meta["workflow"] = workflowFactory([key: meta.config.functionality.name], meta.defaults, meta)
      |
      |// add workflow to environment
      |nextflow.script.ScriptMeta.current().addDefinition(meta.workflow)
      |
      |// anonymous workflow for running this module as a standalone
      |workflow {
      |  // add id argument if it's not already in the config
      |  def newConfig = deepClone(meta.config)
      |  def newParams = deepClone(params)
      |
      |  def argsContainsId = newConfig.functionality.allArguments.any{it.plainName == "id"}
      |  if (!argsContainsId) {
      |    def idArg = [
      |      'name': '--id',
      |      'required': false,
      |      'type': 'string',
      |      'description': 'A unique id for every entry.',
      |      'multiple': false
      |    ]
      |    newConfig.functionality.arguments.add(0, idArg)
      |    newConfig = processConfig(newConfig)
      |  }
      |  if (!newParams.containsKey("id")) {
      |    newParams.id = "run"
      |  }
      |
      |  helpMessage(newConfig)
      |
      |  channelFromParams(newParams, newConfig)
      |    // make sure id is not in the state if id is not in the args
      |    | map {id, state ->
      |      if (!argsContainsId) {
      |        [id, state.findAll{k, v -> k != "id"}]
      |      } else {
      |        [id, state]
      |      }
      |    }
      |    | meta.workflow.run(
      |      auto: [ publish: "state" ]
      |    )
      |}
      |
      |// END COMPONENT-SPECIFIC CODE
      |""".stripMargin
  }
}

// vim: tabstop=2:softtabstop=2:shiftwidth=2:expandtab