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

/**
 * Next-gen Platform class for generating Nextflow (DSL2) modules.
 */
@description("Next-gen platform for generating Nextflow modules.")
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
    val functionality = config.functionality
    
    /************************* HEADER *************************/
    val header = Helper.generateScriptHeader(functionality)
      .map(h => Escaper(h, newline = true))
      .mkString("// ", "\n// ", "")

    /************************* SCRIPT *************************/
    val executionCode = functionality.mainScript match {
      // if mainResource is empty (shouldn't be the case)
      case None => ""

      // if mainResource is simply an executable
      case Some(e: Executable) => //" " + e.path.get + " $VIASH_EXECUTABLE_ARGS"
        throw new NotImplementedError("Running executables through a NextflowPlatform is not yet implemented. Create a support ticket to request this functionality if necessary.")

      // if mainResource is a script
      case Some(res) =>
        // todo: also include the bashwrapper checks
        val argsAndMeta = functionality.getArgumentLikesGroupedByDest(
          includeMeta = true,
          filterInputs = true
        )
        val code = res.readWithInjection(argsAndMeta).get
        val escapedCode = Bash.escapeString(code, allowUnescape = true)
          .replace("\\", "\\\\")
          .replace("'''", "\\'\\'\\'")

        // IMPORTANT! difference between code below and BashWrapper:
        // script is stored as `.viash_script.sh`.
        val scriptPath = "$tempscript"

        s"""set -e
          |tempscript=".viash_script.sh"
          |cat > "$scriptPath" << VIASHMAIN
          |$escapedCode
          |VIASHMAIN
          |${res.command(scriptPath)}
          |""".stripMargin
    }

    /************************* JSONS *************************/
    // override container
    val directivesToJson = directives.copy(
      // if a docker platform is defined but the directives.container isn't, use the image of the dockerplatform as default
      container = directives.container orElse containerDirective.map(cd => Left(cd.toMap)),
      // is memory requirements are defined but directives.memory isn't, use that instead
      memory = directives.memory orElse functionality.requirements.memoryAsBytes.map(_.toString + " B"),
      // is cpu requirements are defined but directives.cpus isn't, use that instead
      cpus = directives.cpus orElse functionality.requirements.cpus.map(np => Left(np))
    )
    val jsonPrinter = JsonPrinter.spaces2.copy(dropNullValues = true)
    val dirJson = directivesToJson.asJson.dropEmptyRecursively
    val dirJson2 = if (dirJson.isNull) Json.obj() else dirJson
    val funJson = config.asJson.dropEmptyRecursively
    val funJsonStr = jsonPrinter.print(funJson)
      .replace("\\\\", "\\\\\\\\")
      .replace("\\\"", "\\\\\"")
      .replace("'''", "\\'\\'\\'")
      .grouped(65000) // JVM has a maximum string limit of 65535
      .toList         // see https://stackoverflow.com/a/6856773
      .mkString("'''", "''' + '''", "'''")
    val autoJson = auto.asJson.dropEmptyRecursively

    /************************* MAIN.NF *************************/
    val tripQuo = """""""""


    s"""$header
      |
      |nextflow.enable.dsl=2
      |
      |// Required imports
      |import groovy.json.JsonSlurper
      |
      |// initialise slurper
      |def jsonSlurper = new JsonSlurper()
      |
      |// DEFINE CUSTOM CODE
      |
      |// functionality metadata
      |thisConfig = processConfig(jsonSlurper.parseText($funJsonStr))
      |
      |thisScript = '''$executionCode'''
      |
      |thisDefaultProcessArgs = [
      |  // key to be used to trace the process and determine output names
      |  key: thisConfig.functionality.name,
      |  // fixed arguments to be passed to script
      |  args: [:],
      |  // default directives
      |  directives: jsonSlurper.parseText('''${jsonPrinter.print(dirJson2)}'''),
      |  // auto settings
      |  auto: jsonSlurper.parseText('''${jsonPrinter.print(autoJson)}'''),
      |  // apply a map over the incoming tuple
      |  // example: { tup -> [ tup[0], [input: tup[1].output], tup[2] ] }
      |  map: null,
      |  // apply a map over the ID element of a tuple (i.e. the first element)
      |  // example: { id -> id + "_foo" }
      |  mapId: null,
      |  // apply a map over the data element of a tuple (i.e. the second element)
      |  // example: { data -> [ input: data.output ] }
      |  mapData: null,
      |  // apply a map over the passthrough elements of a tuple (i.e. the tuple excl. the first two elements)
      |  // example: { pt -> pt.drop(1) }
      |  mapPassthrough: null,
      |  // filter the channel
      |  // example: { tup -> tup[0] == "foo" }
      |  filter: null,
      |  // rename keys in the data field of the tuple (i.e. the second element)
      |  // example: [ "new_key": "old_key" ]
      |  renameKeys: null,
      |  // whether or not to print debug messages
      |  debug: $debug
      |]
      |
      |// END CUSTOM CODE""".stripMargin + 
      "\n\n" + NextflowHelper.workflowHelper + 
      "\n\n" + NextflowHelper.vdsl3Helper
  }
}

// vim: tabstop=2:softtabstop=2:shiftwidth=2:expandtab
