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
import io.viash.config.resources.{ScriptInjectionMods, RScript}

object R extends Language {
  val id: String = "r"
  val name: String = "R"
  val extensions: Seq[String] = Seq(".R", ".r")
  val commentStr: String = "#"
  val executor: Seq[String] = Seq("Rscript")
  val viashParseJsonHybridCode: String = Resources.read("languages/r/ViashParseJsonHybrid.R")
  val viashParseJsonCode: String = Resources.read("languages/r/ViashParseJson.R")

  private val logger = Logger("R")

  def generateInjectionMods(argsMetaAndDeps: Map[String, List[Argument[_]]], config: Config): ScriptInjectionMods = {
    // Determine use_jsonlite setting from the RScript resource
    val useJsonlite = config.resources.collectFirst {
      case rs: RScript => rs.use_jsonlite
    }.flatten

    // Select the appropriate parser code based on the use_jsonlite setting:
    //   Some(true)  -> jsonlite only (no fallback)
    //   Some(false) -> hybrid (jsonlite preferred, custom parser fallback), no warning
    //   None        -> hybrid + build-time deprecation warning
    val helperFunctions = useJsonlite match {
      case Some(true) =>
        // jsonlite-only: small wrapper, no fallback parser
        viashParseJsonCode
      case Some(false) =>
        // Hybrid: jsonlite preferred with custom parser fallback, no warning
        viashParseJsonHybridCode
      case None =>
        // Hybrid + build-time deprecation warning
        logger.warn(
          "Deprecation warning: 'use_jsonlite' is not set for r_script resource. " +
          "Currently defaulting to a hybrid mode (jsonlite preferred, built-in parser fallback). " +
          "In a future version of Viash, the default will change to 'use_jsonlite: true', " +
          "which requires the jsonlite R package to be installed. " +
          "Please set 'use_jsonlite: true' or 'use_jsonlite: false' explicitly in your r_script resource to silence this warning."
        )
        viashParseJsonHybridCode
    }
    
    val paramsCode = if (argsMetaAndDeps.nonEmpty) {
      // Parse JSON once and extract all sections
      val parseOnce = "# Parse JSON parameters once and extract all sections\n.viash_json_data <- viash_parse_json()\n"
      
      val extractSections = argsMetaAndDeps.map { case (dest, args) =>
        // Extract the section
        val sectionExtract = s"$dest <- if (is.null(.viash_json_data[['$dest']])) list() else .viash_json_data[['$dest']]"
        
        // Generate type conversions for long arguments (which are parsed as
        // character strings for values > 2^53 to preserve precision)
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

  def generateConfigInjectMods(argsMetaAndDeps: Map[String, List[Argument[_]]], config: Config): ScriptInjectionMods = {
    val paramsCode = argsMetaAndDeps.map { case (dest, params) =>
      val parSet = params.map { par =>
        val value = formatRValue(par)
        s"""  "${par.plainName}" = $value"""
      }

      s"""$dest <- list(
         |${parSet.mkString(",\n")}
         |)""".stripMargin
    }

    ScriptInjectionMods(params = paramsCode.mkString("\n"))
  }

  private def formatRValue(arg: Argument[_]): String = {
    val rawValues = getArgumentValues(arg)
    if (rawValues.isEmpty) return "NULL"

    if (arg.multiple) {
      val formattedValues = rawValues.map(v => formatSingleRValue(arg, v))
      s"c(${formattedValues.mkString(", ")})"
    } else {
      formatSingleRValue(arg, rawValues.headOption.getOrElse(""))
    }
  }

  private def formatSingleRValue(arg: Argument[_], value: String): String = {
    arg match {
      case _: BooleanArgumentBase => if (value.toLowerCase == "true") "TRUE" else "FALSE"
      case _: IntegerArgument => s"${value}L"
      case _: LongArgument => s"bit64::as.integer64('$value')"
      case _: DoubleArgument => value
      case _ => s""""${value.replace("\\", "\\\\").replace("\"", "\\\"")}""""
    }
  }
}
