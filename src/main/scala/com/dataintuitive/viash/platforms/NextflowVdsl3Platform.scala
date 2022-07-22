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
import io.viash.config.Version
import io.viash.helpers.{Docker, Bash, DockerImageInfo, Helper}
import io.viash.helpers.Circe._
import io.viash.platforms.nextflow._
import io.circe.syntax._
import io.circe.{Printer => JsonPrinter, Json, JsonObject}
import shapeless.syntax.singleton

/**
 * Next-gen Platform class for generating NextFlow (DSL2) modules.
 */
case class NextflowVdsl3Platform(
  id: String = "nextflow",
  `type`: String = "nextflow",
  variant: String = "vdsl3",
  
  // nxf params
  directives: NextflowDirectives = NextflowDirectives(),
  auto: NextflowAuto = NextflowAuto(),
  debug: Boolean = false,

  // TODO: solve differently
  container: String = "docker"
) extends NextflowPlatform {
  def escapeText(txt: String): String = {
    Bash.escape(txt, singleQuote = true, newline = true, backtick = false)
  }

  def modifyFunctionality(config: Config, testing: Boolean): Functionality = {
    val functionality = config.functionality
    val condir = containerDirective(config)

    // create main.nf file
    val mainFile = PlainFile(
      dest = Some("main.nf"),
      text = Some(renderMainNf(functionality, condir))
    )
    val nextflowConfigFile = PlainFile(
      dest = Some("nextflow.config"),
      text = Some(renderNextflowConfig(functionality, condir))
    )

    // remove main
    val otherResources = functionality.additionalResources

    functionality.copy(
      resources = mainFile :: nextflowConfigFile :: otherResources
    )
  }

  def containerDirective(config: Config): Option[DockerImageInfo] = {
    val plat = config.platforms.find(p => p.id == container)
    plat match {
      case Some(p) if !p.isInstanceOf[DockerPlatform] => 
        throw new RuntimeException(s"NextflowPlatform 'container' variable: Platform $container is not a Docker Platform")
      case Some(pp) if pp.isInstanceOf[DockerPlatform] => 
        val p = pp.asInstanceOf[DockerPlatform]
        Some(Docker.getImageInfo(
          functionality = Some(config.functionality),
          registry = p.target_registry,
          organization = p.target_organization,
          name = p.target_image,
          tag = p.target_tag.map(_.toString),
          namespaceSeparator = p.namespace_separator
        ))
      case None => None
    }
  }

  def renderNextflowConfig(functionality: Functionality, containerDirective: Option[DockerImageInfo]): String = {
    val versStr = functionality.version.map(ver => s"\n  version = '$ver'").getOrElse("")

    val descStr = functionality.description.map{des => 
      val escDes = escapeText(des)
      s"\n  description = '$escDes'"
    }.getOrElse("")

    val authStr = 
      if (functionality.authors.isEmpty) {
        "" 
      } else {
        val escAut = escapeText(functionality.authors.mkString(", "))
        s"\n  author = '$escAut'"
      }

    // TODO: define profiles
    val dockerTemp = 
      if (containerDirective.isEmpty) {
        "" 
      } else {
        "\n\n" + NextflowHelper.profilesHelper
      }

    s"""manifest {
    |  name = '${functionality.name}'
    |  mainScript = 'main.nf'
    |  nextflowVersion = '!>=20.12.1-edge'$versStr$descStr$authStr
    |}$dockerTemp""".stripMargin
  }

  // interpreted from BashWrapper
  def renderMainNf(functionality: Functionality, containerDirective: Option[DockerImageInfo]): String = {
    
    /************************* HEADER *************************/
    val header = Helper.generateScriptHeader(functionality)
      .map(h => Bash.escapeMore(h))
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
        val code = res.readWithInjection(functionality).get
        val escapedCode = Bash.escapeMore(code)
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
      container = directives.container orElse containerDirective.map(cd => Left(cd.toMap))
    )
    val jsonPrinter = JsonPrinter.spaces2.copy(dropNullValues = true)
    val dirJson = directivesToJson.asJson.dropEmptyRecursively()
    val dirJson2 = if (dirJson.isNull) Json.obj() else dirJson
    val funJson = functionality.asJson.dropEmptyRecursively()
    val funJsonStr = jsonPrinter.print(funJson)
      .replace("\\\\", "\\\\\\\\")
      .replace("\\\"", "\\\\\"")
      .replace("'''", "\\'\\'\\'")
    val autoJson = auto.asJson.dropEmptyRecursively()

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
      |thisConfig = processConfig([
      |  functionality: jsonSlurper.parseText('''$funJsonStr''')
      |])
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
