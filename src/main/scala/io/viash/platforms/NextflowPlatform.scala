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
 * A Platform class for generating Nextflow (DSL2) modules.
 */
@description(
  """Platform for generating Nextflow VDSL3 modules.
  |
  |A VDSL3 module can be imported just like any other Nextflow module. Example:
  |
  |```groovy
  |include { mymodule } from 'target/nextflow/mymodule/main.nf'
  |```
  |
  |The module expects the channel events to be tuples containing an 'id' and a 'state': `[id, state]`, where `id` is a unique String and `state` is a `Map[String, Object]`. 
  |After running the module, the result is also a tuple `[id, new_state]`. Example:
  |
  |```groovy
  |Channel.fromList([
  |  ["myid", [input: file("in.txt")]]
  |])
  |  | mymodule
  |```
  |
  |If the input tuple has more than two elements, the elements after the second element are passed through to the output tuple.
  |That is, an input tuple `[id, input, ...]` will result in a tuple `[id, output, ...]` after running the module.
  |For example, an input tuple `["foo", [input: file("in.txt")], "bar"]` will result in an output tuple `["foo", [output: file("out.txt")], "bar"]`.
  |
  |A feature unique to VDSL3 modules is that each module can be customized on-the-fly using the `mymodule.run()` function. Example:
  |
  |```groovy
  |Channel.fromList([
  |  ["myid", [input: file("in.txt")]]
  |])
  |  | mymodule.run(
  |    args: [k: 10],
  |    directives: [cpus: 4, memory: "16 GB"]
  |  )
  |```
  |
  |Possible arguments for `.run()`: 
  |
  |- `key` (`String`): A unique key used to trace the process and help make names of output files unique. Default: the name of the module.
  |
  |- `args` (`Map[String, Object]`): Argument overrides to be passed to the module.
  |
  |- `directives` (`Map[String, Object]`): Custom directives overrides. See the Nextflow documentation for a list of available directives.
  |
  |- `auto` (`Map[String, Boolean]`): Whether to apply certain automated processing steps. Possible settings are:
  |  * `simplifyInput`: If true, if the input tuple is a single file and if the module only has a single input file, the input file will be passed the module accordingly. Default: `true`.
  |  * `simplifyOutput`: If true, if the output tuple is a single file and if the module only has a single output file, the output map will be transformed into a single file. Default: `true`.
  |  * `publish`: If true, the output files will be published to the `params.publishDir` folder. Default: `false`.
  |  * `transcript`: If true, the module's transcript will be published to the `params.transcriptDir` folder. Default: `false`.
  |
  |- `map` (`Function`): Apply a map over the incoming tuple. Example: `{ tup -> [ tup[0], [input: tup[1].output] ] + tup.drop(2) }`. Default: `null`.
  |
  |- `mapId` (`Function`): Apply a map over the ID element of a tuple (i.e. the first element). Example: `{ id -> id + "_foo" }`. Default: `null`.
  |
  |- `mapData` (`Function`): Apply a map over the data element of a tuple (i.e. the second element). Example: `{ data -> [ input: data.output ] }`. Default: `null`.
  |
  |- `mapPassthrough` (`Function`): Apply a map over the passthrough elements of a tuple (i.e. the tuple excl. the first two elements). Example: `{ pt -> pt.drop(1) }`. Default: `null`.
  |
  |- `filter` (`Function`): Filter the channel. Example: `{ tup -> tup[0] == "foo" }`. Default: `null`.
  |
  |- `fromState`: Fetch data from the state and pass it to the module without altering the current state. `fromState` should be `null`, `List[String]`, `Map[String, String]` or a function. 
  |  
  |    - If it is `null`, the state will be passed to the module as is.
  |    - If it is a `List[String]`, the data will be the values of the state at the given keys.
  |    - If it is a `Map[String, String]`, the data will be the values of the state at the given keys, with the keys renamed according to the map.
  |    - If it is a function, the tuple (`[id, state]`) in the channel will be passed to the function, and the result will be used as the data.
  |  
  |  Example: `{ id, state -> [input: state.fastq_file] }`
  |  Default: `null`
  |
  |- `toState`: Determine how the state should be updated after the module has been run. `toState` should be `null`, `List[String]`, `Map[String, String]` or a function.
  |
  |    - If it is `null`, the state will be replaced with the output of the module.
  |    - If it is a `List[String]`, the state will be updated with the values of the data at the given keys.
  |    - If it is a `Map[String, String]`, the state will be updated with the values of the data at the given keys, with the keys renamed according to the map.
  |    - If it is a function, a tuple (`[id, output, state]`) will be passed to the function, and the result will be used as the new state.
  |  
  |  Example: `{ id, output, state -> state + [counts: state.output] }`
  |  Default: `{ id, output, state -> output }`
  |
  |- `debug`: Whether or not to print debug messages. Default: `false`.
  |
  |An example of a Nextflow workflow using multiple VDSL3 modules:
  |
  |```groovy
  |include { mymodule1 } from 'target/nextflow/mymodule1/main.nf'
  |include { mymodule2 } from 'target/nextflow/mymodule2/main.nf'
  |
  |Channel.fromList([
  |  ["myid", [input: file("in.txt")]]
  |])
  |  | mymodule1.run(
  |    args: [k: 10],
  |    directives: [cpus: 4, memory: "16 GB"]
  |    fromState: [input: "input"],
  |    toState: [module1_output: "output"]
  |  )
  |  | mymodule2.run(
  |    fromState: { id, state -> 
  |      [input: state.module1_output, k: 4]
  |    },
  |    auto: [publish: true]
  |  )
  |```
  |""".stripMargin
)
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
      |
      |  // Apply a map over the incoming tuple
      |  // Example: `{ tup -> [ tup[0], [input: tup[1].output] ] + tup.drop(2) }`
      |  map: null,
      |
      |  // Apply a map over the ID element of a tuple (i.e. the first element)
      |  // Example: `{ id -> id + "_foo" }`
      |  mapId: null,
      |
      |  // Apply a map over the data element of a tuple (i.e. the second element)
      |  // Example: `{ data -> [ input: data.output ] }`
      |  mapData: null,
      |
      |  // Apply a map over the passthrough elements of a tuple (i.e. the tuple excl. the first two elements)
      |  // Example: `{ pt -> pt.drop(1) }`
      |  mapPassthrough: null,
      |
      |  // Filter the channel
      |  // Example: `{ tup -> tup[0] == "foo" }`
      |  filter: null,
      |
      |  // Rename keys in the data field of the tuple (i.e. the second element)
      |  // Will likely be deprecated in favour of `fromState`.
      |  // Example: `[ "new_key": "old_key" ]`
      |  renameKeys: null,
      |
      |  // Fetch data from the state and pass it to the module without altering the current state.
      |  // 
      |  // `fromState` should be `null`, `List[String]`, `Map[String, String]` or a function. 
      |  // 
      |  // - If it is `null`, the state will be passed to the module as is.
      |  // - If it is a `List[String]`, the data will be the values of the state at the given keys.
      |  // - If it is a `Map[String, String]`, the data will be the values of the state at the given keys, with the keys renamed according to the map.
      |  // - If it is a function, the tuple (`[id, state]`) in the channel will be passed to the function, and the result will be used as the data.
      |  // 
      |  // Example: `{ id, state -> [input: state.fastq_file] }`
      |  // Default: `null`
      |  fromState: null,
      |
      |  // Determine how the state should be updated after the module has been run.
      |  // 
      |  // `toState` should be `null`, `List[String]`, `Map[String, String]` or a function.
      |  // 
      |  // - If it is `null`, the state will be replaced with the output of the module.
      |  // - If it is a `List[String]`, the state will be updated with the values of the data at the given keys.
      |  // - If it is a `Map[String, String]`, the state will be updated with the values of the data at the given keys, with the keys renamed according to the map.
      |  // - If it is a function, a tuple (`[id, output, state]`) will be passed to the function, and the result will be used as the new state.
      |  //
      |  // Example: `{ id, output, state -> state + [counts: state.output] }`
      |  // Default: `{ id, output, state -> output }`
      |  toState: null,
      |
      |  // Whether or not to print debug messages
      |  // Default: `$debug`
      |  debug: $debug
      |]
      |
      |// END CUSTOM CODE""".stripMargin + 
      "\n\n" + NextflowHelper.workflowHelper + 
      "\n\n" + NextflowHelper.vdsl3Helper
  }
}

// vim: tabstop=2:softtabstop=2:shiftwidth=2:expandtab
