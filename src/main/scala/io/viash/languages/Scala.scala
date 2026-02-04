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
      // Parse JSON once
      val parseOnce = "// Parse JSON parameters\nval _viashJsonData = ViashJsonParser.parseJson()\n\n"
      
      // Generate case class and instance for each section (par, meta, dep)
      val sections = argsMetaAndDeps.map { case (dest, params) =>
        val className = s"Viash${dest.capitalize}"
        
        // Generate case class field types
        val classTypes = params.map { par =>
          val classType = par match {
            case a: BooleanArgumentBase if a.multiple => "List[Boolean]"
            case a: IntegerArgument if a.multiple => "List[Int]"
            case a: LongArgument if a.multiple => "List[Long]"
            case a: DoubleArgument if a.multiple => "List[Double]"
            case a: FileArgument if a.multiple => "List[String]"
            case a: StringArgument if a.multiple => "List[String]"
            // Optional types for non-required, non-flag arguments
            case a: BooleanArgumentBase if !a.required && a.flagValue.isEmpty => "Option[Boolean]"
            case a: IntegerArgument if !a.required => "Option[Int]"
            case a: LongArgument if !a.required => "Option[Long]"
            case a: DoubleArgument if !a.required => "Option[Double]"
            case a: FileArgument if !a.required => "Option[String]"
            case a: StringArgument if !a.required => "Option[String]"
            // Required types
            case _: BooleanArgumentBase => "Boolean"
            case _: IntegerArgument => "Int"
            case _: LongArgument => "Long"
            case _: DoubleArgument => "Double"
            case _: FileArgument => "String"
            case _: StringArgument => "String"
          }
          s"  ${par.plainName}: $classType"
        }
        
        // Generate JSON extraction code for each parameter
        val extractors = params.map { par =>
          val jsonKey = par.plainName
          val extractor = par match {
            // Multiple values - extract as List (handle null as empty list)
            case a: BooleanArgumentBase if a.multiple =>
              s"""_${dest}Json.get("$jsonKey").flatMap(v => Option(v)).map(_.asInstanceOf[List[Any]].map(_.toString.toBoolean)).getOrElse(Nil)"""
            case a: IntegerArgument if a.multiple =>
              s"""_${dest}Json.get("$jsonKey").flatMap(v => Option(v)).map(_.asInstanceOf[List[Any]].map(v => v match { case i: Int => i; case d: Double => d.toInt; case s => s.toString.toInt })).getOrElse(Nil)"""
            case a: LongArgument if a.multiple =>
              s"""_${dest}Json.get("$jsonKey").flatMap(v => Option(v)).map(_.asInstanceOf[List[Any]].map(v => v match { case i: Int => i.toLong; case l: Long => l; case d: Double => d.toLong; case s => s.toString.toLong })).getOrElse(Nil)"""
            case a: DoubleArgument if a.multiple =>
              s"""_${dest}Json.get("$jsonKey").flatMap(v => Option(v)).map(_.asInstanceOf[List[Any]].map(v => v match { case d: Double => d; case i: Int => i.toDouble; case s => s.toString.toDouble })).getOrElse(Nil)"""
            case a: FileArgument if a.multiple =>
              s"""_${dest}Json.get("$jsonKey").flatMap(v => Option(v)).map(_.asInstanceOf[List[Any]].map(_.toString)).getOrElse(Nil)"""
            case a: StringArgument if a.multiple =>
              s"""_${dest}Json.get("$jsonKey").flatMap(v => Option(v)).map(_.asInstanceOf[List[Any]].map(_.toString)).getOrElse(Nil)"""
            
            // Optional values
            case a: BooleanArgumentBase if !a.required && a.flagValue.isEmpty =>
              s"""_${dest}Json.get("$jsonKey").flatMap(v => Option(v)).map(_.toString.toBoolean)"""
            case a: IntegerArgument if !a.required =>
              s"""_${dest}Json.get("$jsonKey").flatMap(v => Option(v)).map(v => v match { case i: Int => i; case d: Double => d.toInt; case s => s.toString.toInt })"""
            case a: LongArgument if !a.required =>
              s"""_${dest}Json.get("$jsonKey").flatMap(v => Option(v)).map(v => v match { case i: Int => i.toLong; case l: Long => l; case d: Double => d.toLong; case s => s.toString.toLong })"""
            case a: DoubleArgument if !a.required =>
              s"""_${dest}Json.get("$jsonKey").flatMap(v => Option(v)).map(v => v match { case d: Double => d; case i: Int => i.toDouble; case s => s.toString.toDouble })"""
            case a: FileArgument if !a.required =>
              s"""_${dest}Json.get("$jsonKey").flatMap(v => Option(v)).map(_.toString)"""
            case a: StringArgument if !a.required =>
              s"""_${dest}Json.get("$jsonKey").flatMap(v => Option(v)).map(_.toString)"""
            
            // Required values (handle null as default value)
            case _: BooleanArgumentBase =>
              s"""_${dest}Json.get("$jsonKey").flatMap(v => Option(v)).map(_.toString.toBoolean).getOrElse(false)"""
            case _: IntegerArgument =>
              s"""_${dest}Json.get("$jsonKey").flatMap(v => Option(v)).map(v => v match { case i: Int => i; case d: Double => d.toInt; case s => s.toString.toInt }).getOrElse(0)"""
            case _: LongArgument =>
              s"""_${dest}Json.get("$jsonKey").flatMap(v => Option(v)).map(v => v match { case i: Int => i.toLong; case l: Long => l; case d: Double => d.toLong; case s => s.toString.toLong }).getOrElse(0L)"""
            case _: DoubleArgument =>
              s"""_${dest}Json.get("$jsonKey").flatMap(v => Option(v)).map(v => v match { case d: Double => d; case i: Int => i.toDouble; case s => s.toString.toDouble }).getOrElse(0.0)"""
            case _: FileArgument =>
              s"""_${dest}Json.get("$jsonKey").flatMap(v => Option(v)).map(_.toString).getOrElse("")"""
            case _: StringArgument =>
              s"""_${dest}Json.get("$jsonKey").flatMap(v => Option(v)).map(_.toString).getOrElse("")"""
          }
          s"  $extractor"
        }
        
        // Generate the case class definition
        val caseClassDef = s"""case class $className(
${classTypes.mkString(",\n")}
)"""
        
        // Generate the JSON extraction and instance creation
        val extraction = s"""val _${dest}Json = _viashJsonData.getOrElse("$dest", Map.empty[String, Any]).asInstanceOf[Map[String, Any]]
val $dest = $className(
${extractors.mkString(",\n")}
)"""
        
        caseClassDef + "\n" + extraction
      }
      
      parseOnce + sections.mkString("\n\n")
    } else {
      ""
    }

    ScriptInjectionMods(
      params = helperFunctions + "\n\n" + paramsCode
    )
  }
}
