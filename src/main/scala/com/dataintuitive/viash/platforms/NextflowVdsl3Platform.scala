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

package com.dataintuitive.viash.platforms

import com.dataintuitive.viash.config.Config
import com.dataintuitive.viash.functionality._
import com.dataintuitive.viash.functionality.resources._
import com.dataintuitive.viash.functionality.dataobjects._
import com.dataintuitive.viash.config.Version
import com.dataintuitive.viash.helpers.{Docker, Bash, DockerImageInfo, Helper}
import com.dataintuitive.viash.helpers.Circe._
import com.dataintuitive.viash.platforms.nextflow._
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

  def modifyFunctionality(config: Config): Functionality = {
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
    val otherResources = functionality.resources.tail

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
        s"""
        |
        |// detect tempdir
        |tempDir = java.nio.file.Paths.get(
        |  System.getenv('NXF_TEMP') ?:
        |    System.getenv('VIASH_TEMP') ?: 
        |    System.getenv('TEMPDIR') ?: 
        |    System.getenv('TMPDIR') ?: 
        |    '/tmp'
        |).toAbsolutePath()
        |
        |profiles {
        |  docker {
        |    docker.enabled         = true
        |    docker.userEmulation   = true
        |    docker.temp            = tempDir
        |    singularity.enabled    = false
        |    podman.enabled         = false
        |    shifter.enabled        = false
        |    charliecloud.enabled   = false
        |  }
        |  singularity {
        |    singularity.enabled    = true
        |    singularity.autoMounts = true
        |    docker.enabled         = false
        |    podman.enabled         = false
        |    shifter.enabled        = false
        |    charliecloud.enabled   = false
        |  }
        |  podman {
        |    podman.enabled         = true
        |    podman.temp            = tempDir
        |    docker.enabled         = false
        |    singularity.enabled    = false
        |    shifter.enabled        = false
        |    charliecloud.enabled   = false
        |  }
        |  shifter {
        |    shifter.enabled        = true
        |    docker.enabled         = false
        |    singularity.enabled    = false
        |    podman.enabled         = false
        |    charliecloud.enabled   = false
        |  }
        |  charliecloud {
        |    charliecloud.enabled   = true
        |    charliecloud.temp      = tempDir
        |    docker.enabled         = false
        |    singularity.enabled    = false
        |    podman.enabled         = false
        |    shifter.enabled        = false
        |  }
        |}""".stripMargin
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

    /************************* FUNCTIONALITY *************************/
    val argumentsStr = functionality.allArguments.map{ arg => 
      val descrStr = arg.description.map{des => 
        val escDes = escapeText(des)
        s"\n      'description': '$escDes',"
      }.getOrElse("")

      // construct data for default
      val defTup = 
        if (arg.isInstanceOf[FileObject] && arg.direction == Output) {
          val mult = if (arg.multiple) "_*" else ""
          val (lef, rig) = if (arg.multiple) ("['", "']") else ("'", "'")
          val ExtReg = ".*(\\.[^\\.]*)".r
          val ext = 
            if (arg.default.nonEmpty) {
              arg.default.map(_.toString).toList match {
                case ExtReg(ext) :: _ => ext
                case _ => ""
              }
            } else if (arg.example.nonEmpty) {
              arg.example.map(_.toString).toList match {
                case ExtReg(ext) :: _ => ext
                case _ => ""
              }
            } else {
              ""
            }
          (lef, Some(s"$$id.$$key.${arg.plainName}${mult}${ext}"), rig, false)
        } else if (arg.isInstanceOf[BooleanObject] && arg.asInstanceOf[BooleanObject].flagValue.isDefined) {
          ("", Some((!arg.asInstanceOf[BooleanObject].flagValue.get).toString), "", false)
        } else if (arg.default.isEmpty) {
          ("", None, "", false)
        } else if (arg.multiple && (arg.isInstanceOf[StringObject] || arg.isInstanceOf[FileObject]) ) {
          ("['", Some(arg.default.toList.mkString("', '")), "']", true)
        } else if (arg.multiple) {
          ("[", Some(arg.default.toList.mkString(", ")), "]", true)
        } else if (arg.isInstanceOf[StringObject] || arg.isInstanceOf[FileObject]) {
          ("'", Some(arg.default.head.toString), "'", true)
        } else {
          ("", Some(arg.default.head.toString), "", true)
        }
      // format default as string
      val defaultStr = defTup match {
        case (_, None, _, _) => ""
        case (left, Some(middle), right, escape) =>
          val middleEsc = if (escape) escapeText(middle) else middle
          s"\n      'default': $left$middleEsc$right,"
      }

      val multipleSepStr = if (arg.multiple) s",\n      'multiple_sep': '${arg.multiple_sep}'" else ""

      // construct data for example
      val exaTup = 
        if (arg.example.isEmpty) {
          ("", None, "", false)
        } else if (arg.multiple && (arg.isInstanceOf[StringObject] || arg.isInstanceOf[FileObject]) ) {
          ("['", Some(arg.example.toList.mkString("', '")), "']", true)
        } else if (arg.multiple) {
          ("[", Some(arg.example.toList.mkString(", ")), "]", true)
        } else if (arg.isInstanceOf[StringObject] || arg.isInstanceOf[FileObject]) {
          ("'", Some(arg.example.head.toString), "'", true)
        } else {
          ("", Some(arg.example.head.toString), "", true)
        }
      // format example as string
      val exampleStr = exaTup match {
        case (_, None, _, _) => ""
        case (left, Some(middle), right, escape) =>
          val middleEsc = if (escape) escapeText(middle) else middle
          s"\n      'example': $left$middleEsc$right,"
      }

      s"""
         |    [
         |      'name': '${arg.plainName}',
         |      'required': ${arg.required},
         |      'type': '${arg.`type`}',
         |      'direction': '${arg.direction.toString.toLowerCase}',${descrStr}${defaultStr}${exampleStr}
         |      'multiple': ${arg.multiple}${multipleSepStr}
         |    ]""".stripMargin
    }

    /************************* HELP *************************/
    val helpParams = functionality.allArguments.map {
      case arg => arg.copyDO(
        name = "--" + arg.plainName,
        alternatives = Nil
      )
    }
    val help = Helper.generateHelp(functionality, helpParams)
    val helpStr = help
      .map(h => h.replace("'''", "\\'\\'\\'").replace("\\", "\\\\"))
      .mkString("\n")

    /************************* SCRIPT *************************/
    val executionCode = functionality.mainScript match {
      // if mainResource is empty (shouldn't be the case)
      case None => ""

      // if mainResource is simply an executable
      case Some(e: Executable) => //" " + e.path.get + " $VIASH_EXECUTABLE_ARGS"
        throw new NotImplementedError("Running executables through a NextflowPlatform is not yet implemented. Create a support ticket to request this functionality if necessary.")

      // if mainResource is a script
      case Some(res) =>
        val code = res.readWithPlaceholder(functionality).get
        val escapedCode = Bash.escapeMore(code)
          .replace("'''", "\\'\\'\\'")
          .replace("\\", "\\\\")

        // IMPORTANT! difference between code below and BashWrapper:
        // script is stored as `.viash_script.sh`.
        val scriptPath = "$tempscript"

        s"""set -e
          |tempscript=".viash_script.sh"
          |cat > "$scriptPath" << VIASHMAIN
          |$escapedCode
          |VIASHMAIN
          |${res.meta.command(scriptPath)}
          |""".stripMargin
    }

    /************************* MAIN.NF *************************/
    // override container
    val directives2 = directives.copy(
      container = directives.container orElse containerDirective.map(cd => Left(cd.toMap))
    )
    val tripQuo = """""""""
    val jsonPrinter = JsonPrinter.spaces2.copy(dropNullValues = true)
    val dirJson = directives2.asJson.dropEmptyRecursively()

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
      |thisFunctionality = [
      |  'name': '${functionality.name}',
      |  'arguments': [${argumentsStr.mkString(",")}
      |  ]
      |]
      |
      |thisHelpMessage = '''$helpStr'''
      |
      |thisScript = '''$executionCode'''
      |
      |thisDefaultProcessArgs = [
      |  // key to be used to trace the process and determine output names
      |  key: thisFunctionality.name + "_run",
      |  // fixed arguments to be passed to script
      |  args: [:],
      |  // default directives
      |  directives: jsonSlurper.parseText($tripQuo${jsonPrinter.print(dirJson)}$tripQuo),
      |  // auto settings
      |  auto: jsonSlurper.parseText($tripQuo${jsonPrinter.print(auto.asJson.dropEmptyRecursively())}$tripQuo),
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
      |// END CUSTOM CODE
      |
      |""".stripMargin + NextflowHelper.code
  }
}

// vim: tabstop=2:softtabstop=2:shiftwidth=2:expandtab
