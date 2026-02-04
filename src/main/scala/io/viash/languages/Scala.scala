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

object Scala extends Language {
  val id: String = "scala"
  val name: String = "Scala"
  val extensions: Seq[String] = Seq(".scala")
  val commentStr: String = "//"
  val executor: Seq[String] = Seq("scala", "-nc")
  val viashParseJsonCode: String = Resources.read("languages/scala/ViashParseJson.scala")

  def generateInjectionMods(argsMetaAndDeps: Map[String, List[Argument[_]]], config: Config): ScriptInjectionMods = {
    // Extract only the object and functions, not the main execution part
    val helperFunctions = viashParseJsonCode
      .split("\n")
      .takeWhile(line => !line.contains("if (sys.props.get(\"viash.run.main\").contains(\"true\")"))
      .mkString("\n")
    
    val paramsCode = if (argsMetaAndDeps.nonEmpty) {
      // Parse JSON once and extract all sections
      val parseOnce = "// Parse JSON parameters once and extract all sections\nval _viashJsonData = ViashJsonParser.parseJson()\n"
      val extractSections = argsMetaAndDeps.map { case (dest, _) =>
        s"val $dest = _viashJsonData.getOrElse(\"$dest\", Map.empty[String, Any])"
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
