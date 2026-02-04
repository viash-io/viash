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

object R extends Language {
  val id: String = "r"
  val name: String = "R"
  val extensions: Seq[String] = Seq(".R", ".r")
  val commentStr: String = "#"
  val executor: Seq[String] = Seq("Rscript")
  val viashParseYamlCode: String = Resources.read("languages/r/ViashParseYaml.R")
  val viashParseJsonCode: String = Resources.read("languages/r/ViashParseJson.R")

  def generateInjectionMods(argsMetaAndDeps: Map[String, List[Argument[_]]], config: Config): ScriptInjectionMods = {
    // Extract only the functions, not the main execution part  
    val helperFunctions = viashParseJsonCode
      .split("\n")
      .takeWhile(line => !line.contains("if (!interactive() && identical(environment(), globalenv()))"))
      .mkString("\n")
    
    val paramsCode = if (argsMetaAndDeps.nonEmpty) {
      // Parse JSON once and extract all sections
      val parseOnce = "# Parse JSON parameters once and extract all sections\n.viash_json_data <- viash_parse_json()\n"
      
      val extractSections = argsMetaAndDeps.map { case (dest, args) =>
        // Extract the section
        val sectionExtract = s"$dest <- if (is.null(.viash_json_data[['$dest']])) list() else .viash_json_data[['$dest']]"
        
        // Generate type conversions for long arguments (which come in as character due to bigint_as_char = TRUE)
        val longConversions = args.collect {
          case arg: LongArgument =>
            val name = arg.plainName
            if (arg.multiple) {
              s"if (!is.null($dest[['$name']])) $dest[['$name']] <- bit64::as.integer64($dest[['$name']])"
            } else {
              s"if (!is.null($dest[['$name']])) $dest[['$name']] <- bit64::as.integer64($dest[['$name']])"
            }
        }
        
        if (longConversions.nonEmpty) {
          sectionExtract + "\n" + longConversions.mkString("\n")
        } else {
          sectionExtract
        }
      }.mkString("\n")
      
      parseOnce + extractSections
    } else {
      ""
    }

    val outCode = s"""# treat warnings as errors
       |.viash_orig_warn <- options(warn = 2)
       |
       |$helperFunctions
       |
       |$paramsCode
       |
       |# restore original warn setting
       |options(.viash_orig_warn)
       |rm(.viash_orig_warn)
       |rm(.viash_json_data)
       |""".stripMargin
    ScriptInjectionMods(params = outCode)
  }
}
