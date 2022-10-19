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

package io.viash.schemas

import scala.reflect.runtime.universe._

final case class ParameterSchema(
  name: String,
  `type`: String,
  description: Option[String],
  example: Option[List[ExampleSchema]],
  since: Option[String],
  deprecated: Option[DeprecatedOrRemovedSchema],
  removed: Option[DeprecatedOrRemovedSchema],
)

object ParameterSchema {
  // Aid processing `augmentString` strings
  private def unfinishedStringStripMargin(s: String, marginChar: Char = '|'): String = {
    s.replaceAll("\\\\n", "\n").stripMargin(marginChar)
  }

  private def mapTreeList(l: List[Tree], marginChar: Char = '|'): String = {
    l.map(i => i match {
      case Literal(Constant(value: String)) =>
        unfinishedStringStripMargin(value, marginChar)
      case _ =>
        "unmatched in mapTreeList: " + i.toString()
    }).mkString
  }

  // Traverse tree information and extract values or lists of values
  private def annotationToStrings(ann: Annotation):(String, List[String]) = {
    val name = ann.tree.tpe.toString()
    val values = ann.tree match {
      case Apply(c, args: List[Tree]) =>
        args.collect({
          case i: Tree =>
            i match {
              // Here 'Apply' contains lists
              // While 'Select' has a single element
              case Literal(Constant(value: String)) =>
                value
              // case Select(Select(a, b), stripMargin) =>
              //   unfinishedStringStripMargin(b)
              case Select(Apply(a, a2), b) if b.toString == "stripMargin" =>
                mapTreeList(a2)
              case Apply(Select(Apply(a, a2), b), stripMargin) if b.toString == "stripMargin" =>
                val stripper = stripMargin.head.toString.charAt(1)
                mapTreeList(a2, stripper)
              case _ =>
                "unmatched in annotationToStrings: " + i.toString()
            }
        })
    }
    (name, values)
  }

  def apply(name: String, `type`: String, annotations: List[Annotation]): ParameterSchema = {
    // name is e.g. "io.viash.functionality.Functionality.name", only keep "name"
    // name can also be "__this__"
    val name_ = name.split('.').last
    val annStrings = annotations.map(annotationToStrings(_))

    val description = annStrings.collectFirst({case (name, value) if name.endsWith("description") => value.head})
    val example = annStrings.collect({case (name, value) if name.endsWith("example") => value}).map(ExampleSchema(_))
    val exampleWithDescription = annStrings.collect({case (name, value) if name.endsWith("exampleWithDescription") => value}).map(ExampleSchema(_))
    val examples = example ::: exampleWithDescription match {
      case l if l.length > 0 => Some(l)
      case _ => None
    }
    val since = annStrings.collectFirst({case (name, value) if name.endsWith("since") => value.head})
    val deprecated = annStrings.collectFirst({case (name, value) if name.endsWith("deprecated") => value}).map(DeprecatedOrRemovedSchema(_))
    val removed = annStrings.collectFirst({case (name, value) if name.endsWith("removed") => value}).map(DeprecatedOrRemovedSchema(_))
    ParameterSchema(name_, `type`, description, examples, since, deprecated, removed)
  }
}

final case class DeprecatedOrRemovedSchema(
  message: String,
  since: String,
)

object DeprecatedOrRemovedSchema {
  def apply(l: List[String]): DeprecatedOrRemovedSchema = {
    DeprecatedOrRemovedSchema(l(0), l(1))
  }
}

final case class ExampleSchema(
  example: String,
  format: String,
  description: Option[String],
)

object ExampleSchema {
  def apply(l: List[String]): ExampleSchema = {
    l match {
      case _ if l.length == 2 => ExampleSchema(l(0), l(1), None)
      case _ if l.length == 3 => ExampleSchema(l(0), l(1), Some(l(2)))
      case _ => ???
    }
    
  }
}