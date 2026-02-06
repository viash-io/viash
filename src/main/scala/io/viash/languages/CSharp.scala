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

object CSharp extends Language {
  val id: String = "csharp"
  val name: String = "C#"
  val extensions: Seq[String] = Seq(".csx", ".cs")
  val commentStr: String = "//"
  val executor: Seq[String] = Seq("dotnet", "script")
  val viashParseJsonCode: String = Resources.read("languages/csharp/ViashParseJson.csx")

  def generateInjectionMods(argsMetaAndDeps: Map[String, List[Argument[_]]], config: Config): ScriptInjectionMods = {
    // Extract only the class and functions, not the main execution part
    val helperFunctions = viashParseJsonCode
      .split("\n")
      .takeWhile(line => !line.contains("if (Args.Length == 0)"))
      .mkString("\n")
    
    val paramsCode = if (argsMetaAndDeps.nonEmpty) {
      // Parse JSON once
      val parseOnce = "// Parse JSON parameters\nvar _viashJsonData = ViashJsonParser.ParseJson();\n\n"
      
      // Generate anonymous object for each section (par, meta, dep)
      val sections = argsMetaAndDeps.map { case (dest, params) =>
        // Generate JSON extraction and anonymous object creation
        val jsonExtract = s"""var _${dest}Json = _viashJsonData.ContainsKey("$dest") ? (Dictionary<string, object>)_viashJsonData["$dest"] : new Dictionary<string, object>();"""
        
        // Generate field assignments for anonymous object
        val fieldAssignments = params.map { par =>
          val jsonKey = par.plainName
          val fieldAssignment = par match {
            // Multiple values - extract as arrays
            case a: BooleanArgumentBase if a.multiple =>
              s"""  $jsonKey = _${dest}Json.ContainsKey("$jsonKey") && _${dest}Json["$jsonKey"] != null ? ((List<object>)_${dest}Json["$jsonKey"]).Select(x => Convert.ToBoolean(x)).ToArray() : new bool[0]"""
            case a: IntegerArgument if a.multiple =>
              s"""  $jsonKey = _${dest}Json.ContainsKey("$jsonKey") && _${dest}Json["$jsonKey"] != null ? ((List<object>)_${dest}Json["$jsonKey"]).Select(x => Convert.ToInt32(x)).ToArray() : new int[0]"""
            case a: LongArgument if a.multiple =>
              s"""  $jsonKey = _${dest}Json.ContainsKey("$jsonKey") && _${dest}Json["$jsonKey"] != null ? ((List<object>)_${dest}Json["$jsonKey"]).Select(x => Convert.ToInt64(x)).ToArray() : new long[0]"""
            case a: DoubleArgument if a.multiple =>
              s"""  $jsonKey = _${dest}Json.ContainsKey("$jsonKey") && _${dest}Json["$jsonKey"] != null ? ((List<object>)_${dest}Json["$jsonKey"]).Select(x => Convert.ToDouble(x)).ToArray() : new double[0]"""
            case a: FileArgument if a.multiple =>
              s"""  $jsonKey = _${dest}Json.ContainsKey("$jsonKey") && _${dest}Json["$jsonKey"] != null ? ((List<object>)_${dest}Json["$jsonKey"]).Select(x => x?.ToString()).ToArray() : new string[0]"""
            case a: StringArgument if a.multiple =>
              s"""  $jsonKey = _${dest}Json.ContainsKey("$jsonKey") && _${dest}Json["$jsonKey"] != null ? ((List<object>)_${dest}Json["$jsonKey"]).Select(x => x?.ToString()).ToArray() : new string[0]"""
            
            // Optional values (nullable types)
            case a: BooleanArgumentBase if !a.required && a.flagValue.isEmpty =>
              s"""  $jsonKey = _${dest}Json.ContainsKey("$jsonKey") && _${dest}Json["$jsonKey"] != null ? (bool?)Convert.ToBoolean(_${dest}Json["$jsonKey"]) : null"""
            case a: IntegerArgument if !a.required =>
              s"""  $jsonKey = _${dest}Json.ContainsKey("$jsonKey") && _${dest}Json["$jsonKey"] != null ? (int?)Convert.ToInt32(_${dest}Json["$jsonKey"]) : null"""
            case a: LongArgument if !a.required =>
              s"""  $jsonKey = _${dest}Json.ContainsKey("$jsonKey") && _${dest}Json["$jsonKey"] != null ? (long?)Convert.ToInt64(_${dest}Json["$jsonKey"]) : null"""
            case a: DoubleArgument if !a.required =>
              s"""  $jsonKey = _${dest}Json.ContainsKey("$jsonKey") && _${dest}Json["$jsonKey"] != null ? (double?)Convert.ToDouble(_${dest}Json["$jsonKey"]) : null"""
            case a: FileArgument if !a.required =>
              s"""  $jsonKey = _${dest}Json.ContainsKey("$jsonKey") && _${dest}Json["$jsonKey"] != null ? _${dest}Json["$jsonKey"]?.ToString() : null"""
            case a: StringArgument if !a.required =>
              s"""  $jsonKey = _${dest}Json.ContainsKey("$jsonKey") && _${dest}Json["$jsonKey"] != null ? _${dest}Json["$jsonKey"]?.ToString() : null"""
            
            // Required values
            case _: BooleanArgumentBase =>
              s"""  $jsonKey = _${dest}Json.ContainsKey("$jsonKey") && _${dest}Json["$jsonKey"] != null ? Convert.ToBoolean(_${dest}Json["$jsonKey"]) : false"""
            case _: IntegerArgument =>
              s"""  $jsonKey = _${dest}Json.ContainsKey("$jsonKey") && _${dest}Json["$jsonKey"] != null ? Convert.ToInt32(_${dest}Json["$jsonKey"]) : 0"""
            case _: LongArgument =>
              s"""  $jsonKey = _${dest}Json.ContainsKey("$jsonKey") && _${dest}Json["$jsonKey"] != null ? Convert.ToInt64(_${dest}Json["$jsonKey"]) : 0L"""
            case _: DoubleArgument =>
              s"""  $jsonKey = _${dest}Json.ContainsKey("$jsonKey") && _${dest}Json["$jsonKey"] != null ? Convert.ToDouble(_${dest}Json["$jsonKey"]) : 0.0"""
            case _: FileArgument =>
              "  " + jsonKey + " = _" + dest + "Json.ContainsKey(\"" + jsonKey + "\") && _" + dest + "Json[\"" + jsonKey + "\"] != null ? _" + dest + "Json[\"" + jsonKey + "\"]?.ToString() : \"\""
            case _: StringArgument =>
              "  " + jsonKey + " = _" + dest + "Json.ContainsKey(\"" + jsonKey + "\") && _" + dest + "Json[\"" + jsonKey + "\"] != null ? _" + dest + "Json[\"" + jsonKey + "\"]?.ToString() : \"\""
          }
          fieldAssignment
        }
        
        // Generate the anonymous object
        val anonObject = if (params.nonEmpty) {
          s"""var $dest = new {
${fieldAssignments.mkString(",\n")}
};"""
        } else {
          s"var $dest = new {};"
        }
        
        jsonExtract + "\n" + anonObject
      }
      
      parseOnce + sections.mkString("\n\n")
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
        val value = formatCSharpValue(par)
        s"${par.plainName} = $value"
      }

      s"""var $dest = new {
         |  ${parSet.mkString(",\n  ")}
         |};""".stripMargin
    }

    ScriptInjectionMods(params = paramsCode.mkString("\n"))
  }

  private def getCSharpArrayType(arg: Argument[_]): String = {
    arg match {
      case _: BooleanArgumentBase => "bool"
      case _: IntegerArgument => "int"
      case _: LongArgument => "long"
      case _: DoubleArgument => "double"
      case _: FileArgument => "string"
      case _: StringArgument => "string"
    }
  }

  private def formatCSharpValue(arg: Argument[_]): String = {
    val rawValues = getArgumentValues(arg)
    
    if (rawValues.isEmpty) {
      // Return null with appropriate cast
      val class_ = getCSharpArrayType(arg)
      return if (arg.multiple) s"($class_[]) null" else if (arg.isInstanceOf[StringArgument] || arg.isInstanceOf[FileArgument]) s"($class_) null" else s"($class_?) null"
    }

    if (arg.multiple) {
      val arrayType = getCSharpArrayType(arg)
      val formattedValues = rawValues.map(v => formatSingleCSharpValue(arg, v))
      s"new $arrayType[] { ${formattedValues.mkString(", ")} }"
    } else {
      formatSingleCSharpValue(arg, rawValues.headOption.getOrElse(""))
    }
  }

  private def formatSingleCSharpValue(arg: Argument[_], value: String): String = {
    arg match {
      case _: BooleanArgumentBase => if (value.toLowerCase == "true") "true" else "false"
      case _: IntegerArgument | _: LongArgument => value
      case _: DoubleArgument => value
      case _ => s"@\"${value.replace("\"", "\"\"")}\""
    }
  }
}
