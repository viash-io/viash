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

package io.viash.helpers

import io.viash.config.Config
import io.viash.Main
import io.viash.config.ArgumentGroup
import io.viash.config.arguments.{Argument, StringArgument, Output, IntegerArgument, LongArgument, DoubleArgument, FileArgument}
import io.viash.helpers.data_structures.oneOrMoreToList

object Helper {
  private val maxWidth: Int = 80

  def nameAndVersion(config: Config): String = {
    config.name + config.version.map(" " + _).getOrElse("")
  }

  // NOTE! changes to this function should also be ported to WorkflowHelper.nf::generateHelp
  def generateHelp(config: Config): List[String] = {
    // PART 1: NAME AND VERSION
    def nameStr = nameAndVersion(config)

    // PART 2: DESCRIPTION
    val descrStr = config.description.map(des => "\n\n" + Format.paragraphWrap(des.trim, maxWidth).mkString("\n")).getOrElse("")

    // PART 3: Usage
    val usageStr = config.usage.map("\n\nUsage:\n" + _.trim).getOrElse("")

    // PART 4: Options
    val argGroupStrs = config.argument_groups.map{argGroup =>
      val name = argGroup.name
      val descriptionStr = argGroup.description.map{
        des => "\n    " + Format.paragraphWrap(des.trim, maxWidth-4).mkString("\n    ") + "\n"
      }.getOrElse("")
      val argumentStrs = argGroup.arguments.map(param => generateArgumentHelp(param))
      
      s"\n\n$name:" +
      descriptionStr +
      argumentStrs.mkString("\n")
    }

    // FINAL: combine
    val out = nameStr + 
      descrStr +
      usageStr + 
      argGroupStrs.mkString

    Format.paragraphWrap(out, 80).toList
  }

  // NOTE! changes to this function should also be ported to WorkflowHelper.nf::generateArgumentHelp
  def generateArgumentHelp(param: Argument[_]) = {
    val names = param.alternatives ::: List(param.name)

    val unnamedProps = List(
      ("required parameter", param.required),
      ("multiple values allowed", param.multiple),
      ("output", param.direction == Output),
      ("file must exist", param.isInstanceOf[FileArgument] && param.asInstanceOf[FileArgument].must_exist)
    ).filter(_._2).map(_._1)
    
    val default = 
      if (param.default.nonEmpty) {
        Some(param.default.map(_.toString).mkString(param.multiple_sep.toString))
      } else {
        None
      }
    val example = 
      if (param.example.nonEmpty) {
        Some(param.example.map(_.toString).mkString(param.multiple_sep.toString))
      } else {
        None
      }
    val min = param match {
        case p: IntegerArgument if p.min.nonEmpty =>
          p.min.map(_.toString)
        case p: LongArgument if p.min.nonEmpty =>
          p.min.map(_.toString)
        case p: DoubleArgument if p.min.nonEmpty =>
          p.min.map(_.toString)
        case _ =>
          None
      }
    val max = param match {
        case p: IntegerArgument if p.max.nonEmpty =>
          p.max.map(_.toString)
        case p: LongArgument if p.max.nonEmpty =>
          p.max.map(_.toString)
        case p: DoubleArgument if p.max.nonEmpty =>
          p.max.map(_.toString)
        case _ =>
          None
      }

    def escapeChoice(choice: String) = {        
      val s1 = choice.replaceAll("\\n", "\\\\n")
      val s2 = s1.replaceAll("\"", """\\\"""")
      s2 match {
        case s if s.contains(',') || s != choice =>
          "\"" + s + "\""
        case _ =>
          s2
      }
    }
    val choices = 
      param match {
        case so: StringArgument if so.choices != Nil =>
          Some("[ " + so.choices.map(escapeChoice(_)).mkString(", ") + " ]")
        case so: IntegerArgument if so.choices != Nil =>
          Some("[ " + so.choices.mkString(", ") + " ]")
        case so: LongArgument if so.choices != Nil =>
          Some("[ " + so.choices.mkString(", ") + " ]")
        case _ => None
      }

    val namedPropsStr = List(
      ("type", Some((param.`type` :: unnamedProps).mkString(", "))),
      ("default", default),
      ("example", example),
      ("choices", choices),
      ("min", min),
      ("max", max)
    ).flatMap { case (name, x) =>
      x.map("\n        " + name + ": " + _.replaceAll("\n", "\\n"))
    }.mkString
    
    val descStr = param.description.map{ desc =>
      Format.paragraphWrap("\n" + desc.trim, maxWidth-8).mkString("\n        ")
    }.getOrElse("")
    
    "\n    " +
      names.mkString(", ") +
      namedPropsStr +
      descStr
  }

  def generateScriptHeader(config: Config): List[String] = {
        // generate header
    val nav = Helper.nameAndVersion(config)

    val authorHeader =
      if (config.authors.isEmpty) {
        ""
      } else {
        config.authors.map(_.toString).mkString("\n\nComponent authors:\n * ", "\n * ", "")
      }

    val out =
      s"""$nav
         |
         |This wrapper script is auto-generated by ${Main.name} ${Main.version} and is thus a derivative work thereof. This software comes with ABSOLUTELY NO WARRANTY from Data Intuitive.
         |
         |The component may contain files which fall under a different license. The authors of this component should specify the license in the header of such files, or include a separate license file detailing the licenses of all included files.""".stripMargin +
         authorHeader

    Format.paragraphWrap(out, 80).toList
  }
}
