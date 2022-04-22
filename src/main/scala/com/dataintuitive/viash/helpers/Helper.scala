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

package com.dataintuitive.viash.helpers

import com.dataintuitive.viash.functionality.Functionality
import com.dataintuitive.viash.functionality.dataobjects._

object Helper {
  def nameAndVersion(functionality: Functionality): String = {
    functionality.name + functionality.version.map(" " + _).getOrElse(" <not versioned>")
  }

  def generateHelp(functionality: Functionality, params: List[DataObject[_]]): List[String] = {
    // PART 1: NAME AND VERSION
    def nameStr = nameAndVersion(functionality)

    // PART 2: DESCRIPTION
    val descrStr = functionality.description.map("\n\n" + _.strip).getOrElse("")

    // PART 3: Usage
    val usageStr = functionality.usage.map("\n\nUsage:\n" + _.strip).getOrElse("")

    // PART 4: Options
    val paramStrs = params.map(param => {
      val names = param.alternatives ::: List(param.name)

      val unnamedProps = List(
        ("required parameter", param.required),
        ("multiple values allowed", param.multiple),
        ("output", param.direction == Output),
        ("file must exist", param.isInstanceOf[FileObject] && param.asInstanceOf[FileObject].must_exist)
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
      val namedPropsStr = List(
        ("type", Some((param.oType :: unnamedProps).mkString(", "))),
        ("default", default),
        ("example", example)
      ).flatMap { case (name, x) =>
        x.map("\n        " + name + ": " + _.replaceAll("\n", "\\n"))
      }.mkString

      val descStr = param.description.map{ desc =>
        ("\n" + desc.strip).replaceAll("\n", "\n        ")
      }.getOrElse("")
      
      "\n    " +
        names.mkString(", ") +
        namedPropsStr + 
        descStr
    })
    
    val paramStr = if (paramStrs.nonEmpty) "\n\nOptions: " + paramStrs.mkString("\n") else ""

    // FINAL: combine
    val out = nameStr + 
      descrStr +
      usageStr + 
      paramStr

    out.split("\n").toList
  }

  // TODO: move scriptheader from BashWrapper to here!
}
