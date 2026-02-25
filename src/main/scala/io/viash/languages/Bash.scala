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

package io.viash.languages

import io.viash.helpers.{Resources, Logger}
import io.viash.config.arguments._
import io.viash.config.Config
import io.viash.config.resources.{ScriptInjectionMods, BashScript}

object Bash extends Language {
  val id: String = "bash"
  val name: String = "Bash"
  val extensions: Seq[String] = Seq(".sh")
  val commentStr: String = "#"
  val executor: Seq[String] = Seq("bash")
  val viashParseJsonCode: String = Resources.read("languages/bash/ViashParseJson.sh")
  val viashParseJsonCompatCode: String = Resources.read("languages/bash/ViashParseJsonCompatibility.sh")

  private val logger = Logger("Bash")

  def generateInjectionMods(argsMetaAndDeps: Map[String, List[Argument[_]]], config: Config): ScriptInjectionMods = {
    // Determine use_jq setting from the BashScript resource
    val useJq = config.resources.collectFirst {
      case bs: BashScript => bs.use_jq
    }.flatten

    useJq match {
      case Some(true) =>
        val parseCode = s"""|${viashParseJsonCode}
          |
          |# Parse JSON parameters using jq
          |_viash_json_content=$$(cat "$$VIASH_WORK_PARAMS")
          |ViashParseJsonBash <<< "$$_viash_json_content"
          |""".stripMargin
        ScriptInjectionMods(params = parseCode)
      case Some(false) =>
        generateCompatInjectionMods(argsMetaAndDeps)
      case None =>
        logger.warn(
          "Deprecation warning: 'use_jq' is not set for bash_script resource. " +
          "Currently defaulting to compatibility mode (built-in parser, separator-delimited strings for multiple-value arguments). " +
          "In a future version of Viash, the default will change to 'use_jq: true', " +
          "which requires jq to be installed. " +
          "Please set 'use_jq: true' or 'use_jq: false' explicitly in your bash_script resource to silence this warning."
        )
        generateCompatInjectionMods(argsMetaAndDeps)
    }
  }

  /**
   * Generate injection mods for compatibility mode:
   * Uses the built-in bash JSON parser, then converts multiple-value
   * arguments from bash arrays to separator-delimited strings.
   */
  private def generateCompatInjectionMods(argsMetaAndDeps: Map[String, List[Argument[_]]]): ScriptInjectionMods = {
    val parseCode = s"""${viashParseJsonCompatCode}

# Parse JSON parameters
_viash_json_content=$$(cat "$$VIASH_WORK_PARAMS")
ViashParseJsonBash <<< "$$_viash_json_content"
"""

    // Convert multiple-value arguments from arrays to IFS-separated strings.
    // Note: We must unset the array variable before reassigning as a scalar,
    // because assigning a string to a bash array variable only sets index [0]
    // while leaving other indices intact.
    val multipleArgs = argsMetaAndDeps.toList.flatMap { case (_, args) =>
      args.collect {
        case arg if arg.multiple =>
          val sep = arg.multiple_sep
          val par = arg.par
          s"""|_viash_tmp="$$(IFS='${sep}'; printf '%s' "$${${par}[*]}")"
            |unset ${par}
            |${par}="$$_viash_tmp"
            |unset _viash_tmp""".stripMargin
      }
    }

    val fullCode = if (multipleArgs.nonEmpty) {
      parseCode + "\n# Convert arrays to separator-delimited strings for compatibility\n" +
        multipleArgs.mkString("\n") + "\n"
    } else {
      parseCode
    }

    ScriptInjectionMods(params = fullCode)
  }

  def generateConfigInjectMods(argsMetaAndDeps: Map[String, List[Argument[_]]], config: Config): ScriptInjectionMods = {
    // Determine use_jq setting from the BashScript resource
    val useJq = config.resources.collectFirst {
      case bs: BashScript => bs.use_jq
    }.flatten
    val useArrays = useJq.contains(true)

    val parSet = argsMetaAndDeps.flatMap { case (_, params) =>
      params.flatMap { par =>
        val value = getExampleValue(par, useArrays)
        if (value.isEmpty) {
          None
        } else if (par.multiple && par.direction != Output && useArrays) {
          // jq mode with arrays
          Some(s"""${par.par}=($value)""")
        } else {
          Some(s"""${par.par}='$value'""")
        }
      }
    }

    val paramsCode = parSet.mkString("\n")
    ScriptInjectionMods(params = paramsCode)
  }

  private def getExampleValue(arg: Argument[_], useArrays: Boolean = true): String = {
    val values = getArgumentValues(arg)

    if (arg.multiple && arg.direction != Output) {
      if (useArrays) {
        values.map(v => s"'$v'").mkString(" ")
      } else {
        values.mkString(arg.multiple_sep)
      }
    } else {
      values.headOption.getOrElse("")
    }
  }
}
