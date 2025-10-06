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

import io.viash.config.arguments._
import io.viash.wrapper.BashWrapper
import io.viash.schemas._

import java.net.URI
import io.viash.helpers.Bash
import io.viash.config.Config
import io.viash.languages.CSharp

@description("""An executable C# script.
               |When defined in resources, only the first entry will be executed when running the built component or when running `viash run`.
               |When defined in test_resources, all entries will be executed during `viash test`.""")
@subclass("csharp_script")
case class CSharpScript(
  path: Option[String] = None,
  text: Option[String] = None,
  dest: Option[String] = None,
  is_executable: Option[Boolean] = Some(true),
  parent: Option[URI] = None,

  @description("Specifies the resource as a C# script.")
  `type`: String = "csharp_script"
) extends Script {
  val language = CSharp
  def copyResource(path: Option[String], text: Option[String], dest: Option[String], is_executable: Option[Boolean], parent: Option[URI]): Resource = {
    copy(path = path, text = text, dest = dest, is_executable = is_executable, parent = parent)
  }

  def generateInjectionMods(argsMetaAndDeps: Map[String, List[Argument[_]]], config: Config): ScriptInjectionMods = {
    // Extract only the class and functions, not the main execution part
    val helperFunctions = language.viashParseJsonCode
      .split("\n")
      .takeWhile(line => !line.contains("if (Args.Length == 0)"))
      .mkString("\n")
    
    val paramsCode = if (argsMetaAndDeps.nonEmpty) {
      // Parse JSON once and extract all sections
      val parseOnce = "// Parse JSON parameters once and extract all sections\nvar _viashJsonData = ViashJsonParser.ParseJson();\n"
      val extractSections = argsMetaAndDeps.map { case (dest, _) =>
        s"var $dest = _viashJsonData.ContainsKey(\"$dest\") ? (Dictionary<string, object>)_viashJsonData[\"$dest\"] : new Dictionary<string, object>();"
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
