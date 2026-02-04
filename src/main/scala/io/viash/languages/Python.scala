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

object Python extends Language {
  val id: String = "python"
  val name: String = "Python"
  val extensions: Seq[String] = Seq(".py")
  val commentStr: String = "#"
  val executor: Seq[String] = Seq("python", "-B")
  val viashParseJsonCode: String = Resources.read("languages/python/ViashParseJson.py")

  def generateInjectionMods(argsMetaAndDeps: Map[String, List[Argument[_]]], config: Config): ScriptInjectionMods = {
    // Extract only the functions, not the main execution part
    val helperFunctions = viashParseJsonCode
      .split("\n")
      .takeWhile(line => !line.startsWith("if __name__ == \"__main__\":"))
      .mkString("\n")
    
    val paramsCode = if (argsMetaAndDeps.nonEmpty) {
      // Parse JSON once and extract all sections
      val parseOnce = "# Parse JSON parameters once and extract all sections\n_viash_json_data = viash_parse_json()\n"
      val extractSections = argsMetaAndDeps.map { case (dest, _) =>
        s"$dest = _viash_json_data.get('$dest', {})"
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
