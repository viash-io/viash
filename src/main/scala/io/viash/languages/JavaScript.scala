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

import io.viash.helpers.Resources
import io.viash.config.arguments._
import io.viash.config.Config
import io.viash.config.resources.ScriptInjectionMods

object JavaScript extends Language {
  val id: String = "javascript"
  val name: String = "JavaScript"
  val extensions: Seq[String] = Seq(".js")
  val commentStr: String = "//"
  val executor: Seq[String] = Seq("node")
  val viashParseJsonCode: String = Resources.read("languages/javascript/ViashParseJson.js")

  def generateInjectionMods(argsMetaAndDeps: Map[String, List[Argument[_]]], config: Config): ScriptInjectionMods = {
    // Extract only the functions, not the main execution part or module exports
    val helperFunctions = viashParseJsonCode
      .split("\n")
      .takeWhile(line => !line.contains("if (require.main === module)"))
      .filterNot(line => line.contains("module.exports"))
      .mkString("\n")
    
    val paramsCode = if (argsMetaAndDeps.nonEmpty) {
      // Parse JSON once and extract all sections
      val parseOnce = "// Parse JSON parameters once and extract all sections\nconst _viashJsonData = viashParseJson();\n"
      val extractSections = argsMetaAndDeps.map { case (dest, _) =>
        s"const $dest = _viashJsonData['$dest'] || {};"
      }.mkString("\n")
      
      parseOnce + extractSections
    } else {
      ""
    }
    
    ScriptInjectionMods(
      params = helperFunctions + "\n\n" + paramsCode
    )
  }

  def generateConfigInjectMods(argsMetaAndDeps: Map[String, List[Argument[_]]], config: Config): ScriptInjectionMods = {
    val paramsCode = argsMetaAndDeps.map { case (dest, params) =>
      val parSet = params.map { par =>
        val value = formatJSValue(par)
        s"  '${par.plainName}': $value"
      }

      s"""let $dest = {
         |${parSet.mkString(",\n")}
         |};""".stripMargin
    }

    ScriptInjectionMods(params = paramsCode.mkString("\n"))
  }

  private def formatJSValue(arg: Argument[_]): String = {
    // Priority: example > default > undefined for optional args
    val rawValues = arg.example.toList match {
      case Nil => arg.default.toList match {
        case Nil => return "undefined"
        case defaults => defaults.map(_.toString)
      }
      case examples => examples.map(_.toString)
    }

    if (arg.multiple) {
      val formattedValues = rawValues.map(v => formatSingleJSValue(arg, v))
      s"[${formattedValues.mkString(", ")}]"
    } else {
      formatSingleJSValue(arg, rawValues.headOption.getOrElse(""))
    }
  }

  private def formatSingleJSValue(arg: Argument[_], value: String): String = {
    arg match {
      case _: BooleanArgumentBase => if (value.toLowerCase == "true") "true" else "false"
      case _: IntegerArgument | _: LongArgument => value
      case _: DoubleArgument => value
      case _ => s"String.raw`${value.replace("`", "\\`")}`"
    }
  }
}
