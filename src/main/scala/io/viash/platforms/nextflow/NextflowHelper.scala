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

package io.viash.platforms.nextflow

import scala.io.Source
import io.viash.config.Config

import io.circe.syntax._
import io.viash.helpers.circe._
import io.viash.helpers.Helper
import io.viash.helpers.Escaper
import io.viash.helpers.Bash
import io.viash.helpers.DockerImageInfo
import io.circe.{Printer => JsonPrinter, Json, JsonObject}
import io.viash.functionality.dependencies.Dependency
import java.nio.file.Path
import java.nio.file.Paths
import io.viash.ViashNamespace

object NextflowHelper {
  private def readSource(s: String) = {
    val path = s"io/viash/platforms/nextflow/$s"
    Source.fromResource(path).getLines().mkString("\n")
  }

  lazy val vdsl3Helper: String = readSource("VDSL3Helper.nf")
  lazy val workflowHelper: String = readSource("WorkflowHelper.nf")
  lazy val profilesHelper: String = readSource("ProfilesHelper.config")
  lazy val dataflowHelper: String = readSource("DataflowHelper.nf")

  // cat src/main/resources/io/viash/platforms/nextflow/*/*.nf > src/main/resources/io/viash/platforms/nextflow/WorkflowHelper.nf

  def generateConfigStr(config: Config): String = {
    val configJson = config.asJson.dropEmptyRecursively
    val configJsonStr = configJson
      .toFormattedString("json")
      .replace("\\\\", "\\\\\\\\")
      .replace("\\\"", "\\\\\"")
      .replace("'''", "\\'\\'\\'")
      .grouped(65000) // JVM has a maximum string limit of 65535
      .toList         // see https://stackoverflow.com/a/6856773
      .mkString("'''", "''' + '''", "'''")

    s"processConfig(readJsonBlob($configJsonStr))"
  }

  def generateScriptStr(config: Config): String = {
    val res = config.functionality.mainScript.get

    // todo: also include the bashwrapper checks
    val argsAndMeta = config.functionality.getArgumentLikesGroupedByDest(
      includeMeta = true,
      filterInputs = true
    )
    val code = res.readWithInjection(argsAndMeta, config).get
    val escapedCode = Bash.escapeString(code, allowUnescape = true)
      .replace("\\", "\\\\")
      .replace("'''", "\\'\\'\\'")

    // IMPORTANT! difference between code below and BashWrapper:
    // script is stored as `.viash_script.sh`.
    val scriptPath = "$tempscript"

    val executionCode = 
      s"""set -e
        |tempscript=".viash_script.sh"
        |cat > "$scriptPath" << VIASHMAIN
        |$escapedCode
        |VIASHMAIN
        |${res.command(scriptPath)}
        |""".stripMargin
    
    s"'''$executionCode'''"
  }

  def generateDefaultProcessArgs(config: Config, directives: NextflowDirectives, auto: NextflowAuto, debug: Boolean): String = {
    // override container
    val jsonPrinter = JsonPrinter.spaces2.copy(dropNullValues = true)
    val dirJson = directives.asJson.dropEmptyRecursively
    val dirJson2 = if (dirJson.isNull) Json.obj() else dirJson

    val autoJson = auto.asJson.dropEmptyRecursively

    s"""[
      |  // key to be used to trace the process and determine output names
      |  key: thisConfig.functionality.name,
      |  // fixed arguments to be passed to script
      |  args: [:],
      |  // default directives
      |  directives: readJsonBlob('''${jsonPrinter.print(dirJson2)}'''),
      |  // auto settings
      |  auto: readJsonBlob('''${jsonPrinter.print(autoJson)}'''),
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
      |]""".stripMargin
  }

  def generateHeader(config: Config): String = {
    Helper.generateScriptHeader(config.functionality)
      .map(h => Escaper(h, newline = true))
      .mkString("// ", "\n// ", "")
  }

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

  def renderDependencies(config: Config): String = {
    // TODO ideally we'd already have 'thisPath' precalculated but until that day, calculate it here
    val thisPath = Paths.get(ViashNamespace.targetOutputPath("", "invalid_platform_name", config.functionality.namespace, config.functionality.name))

    val depStrs = config.functionality.dependencies.map{ dep =>
      NextflowHelper.renderInclude(dep, thisPath)
    }

    if (depStrs.isEmpty) {
      return ""
    }

    s"""
      |// import dependencies
      |rootDir = getRootDir()
      |${depStrs.mkString("\n|")}
      |""".stripMargin
  }
}
