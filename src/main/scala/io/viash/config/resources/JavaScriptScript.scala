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

package io.viash.config.resources

import io.viash.wrapper.BashWrapper
import io.viash.schemas._

import java.net.URI
import io.viash.helpers.Bash
import io.viash.config.Config
import io.viash.config.arguments.{Argument, StringArgument, IntegerArgument, BooleanArgumentBase, LongArgument, DoubleArgument, FileArgument}
import io.viash.languages.JavaScript

@description("""An executable JavaScript script.
               |When defined in resources, only the first entry will be executed when running the built component or when running `viash run`.
               |When defined in test_resources, all entries will be executed during `viash test`.""")
@subclass("javascript_script")
case class JavaScriptScript(
  path: Option[String] = None,
  text: Option[String] = None,
  dest: Option[String] = None,
  is_executable: Option[Boolean] = Some(true),
  parent: Option[URI] = None,

  @description("Specifies the resource as a JavaScript script.")
  `type`: String = "javascript_script"
) extends Script {
  val language = JavaScript
  def copyResource(path: Option[String], text: Option[String], dest: Option[String], is_executable: Option[Boolean], parent: Option[URI]): Resource = {
    copy(path = path, text = text, dest = dest, is_executable = is_executable, parent = parent)
  }

  def generateInjectionMods(argsMetaAndDeps: Map[String, List[Argument[_]]], config: Config): ScriptInjectionMods = {
    // Extract only the functions, not the main execution part or module exports
    val helperFunctions = language.viashParseJsonCode
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
}
