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

object Bash extends Language {
  val id: String = "bash"
  val name: String = "Bash"
  val extensions: Seq[String] = Seq(".sh")
  val commentStr: String = "#"
  val executor: Seq[String] = Seq("bash")
  val viashParseJsonCode: String = Resources.read("languages/bash/ViashParseJson.sh")

  def generateInjectionMods(argsMetaAndDeps: Map[String, List[Argument[_]]], config: Config): ScriptInjectionMods = {
    val fullCode = s"""${viashParseJsonCode}

# Parse JSON parameters
_viash_json_content=$$(cat "$$VIASH_WORK_PARAMS")
ViashParseJsonBash <<< "$$_viash_json_content"

"""
    ScriptInjectionMods(params = fullCode)
  }
}
