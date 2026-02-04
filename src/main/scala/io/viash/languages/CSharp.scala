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
import io.viash.config.arguments.Argument
import io.viash.config.Config
import io.viash.config.resources.ScriptInjectionMods

object CSharp extends Language {
  val id: String = "csharp"
  val name: String = "C#"
  val extensions: Seq[String] = Seq(".csx", ".cs")
  val commentStr: String = "//"
  val executor: Seq[String] = Seq("dotnet", "script")
  val viashParseYamlCode: String = Resources.read("languages/csharp/ViashParseYaml.csx")
  val viashParseJsonCode: String = Resources.read("languages/csharp/ViashParseJson.csx")

  def generateInjectionMods(argsMetaAndDeps: Map[String, List[Argument[_]]], config: Config): ScriptInjectionMods = {
    // Extract only the class and functions, not the main execution part
    val helperFunctions = viashParseJsonCode
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
