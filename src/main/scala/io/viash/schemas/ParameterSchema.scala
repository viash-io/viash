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

import io.viash.schemas.internalFunctionality
import scala.annotation.Annotation

final case class ParameterSchema(
  name: String,
  `type`: String,
  niceType: String,
  hierarchy: Option[List[String]],
  description: Option[String],
  example: Option[List[ExampleSchema]],
  since: Option[String],
  deprecated: Option[DeprecatedOrRemovedSchema],
  removed: Option[DeprecatedOrRemovedSchema],
  default: Option[String],
  subclass: Option[List[String]],
  @internalFunctionality
  hasUndocumented: Boolean,
  @internalFunctionality
  hasInternalFunctionality: Boolean,
)

object ParameterSchema {

  def apply(name: String, `type`: String, hierarchy: List[String], annotations: List[(String, List[String])]): ParameterSchema = {

    def beautifyTypeName(s: String): String = {

      // "tpe[a]" -> "(\w*)\[(\w*)\]"
      def regexify(s: String) = s.replace("[", "\\[").replace("]", "\\]").replaceAll("\\w+", "(\\\\w+)").r

      val regex0 = regexify("tpe")
      val regex1 = regexify("tpe[a]")
      val regex2 = regexify("tpe[a,b]")
      val regexNested1 = regexify("tpe[a[b,c]]")
      val regexNested2 = regexify("tpe[a[b,c[d,e]]]")
      val regexNested3 = regexify("tpe[a,b[c,d]]")
      val regexNested4 = regexify("tpe[a[b[c,d],e]]")
      val regexNested5 = regexify("tpe[a[b,c[d]]]")
      val regexNested6 = regexify("tpe[a[b,c],d]")
      val regexNested7 = regexify("tpe[a,b[c]]")

      def map(a: String, b: String): String = s"Map of $a to $b"
      def either(a: String, b: String): String = s"""Either $a or $b"""
      
      s match {
        case regex0(tpe) => s"$tpe"
        case regex1(tpe, subtpe) => s"$tpe of $subtpe"
        case regex2("Map", subtpe1, subtpe2) => map(subtpe1,subtpe2)
        case regex2("ListMap", subtpe1, subtpe2) => map(subtpe1, subtpe2)
        case regex2("Either", subtpe1, subtpe2) => either(subtpe1, subtpe2)
        case regexNested1(tpe, a, b, c) => s"$tpe of ${beautifyTypeName(s"$a[$b,$c]")}"
        case regexNested2(tpe, a, b ,c ,d, e) => s"$tpe of ${beautifyTypeName(s"$a[$b,$c[$d,$e]]")}"
        case regexNested3("Either", a, b, c, d) => either(beautifyTypeName(a), beautifyTypeName(s"$b[$c,$d]"))
        case regexNested4(tpe, a, b, c, d, e) => s"$tpe of ${beautifyTypeName(s"$a[$b[$c,$d],$e]")}"
        case regexNested5(tpe, a, b, c, d) => s"$tpe of ${beautifyTypeName(s"$a[$b,$c[$d]]")}"
        case regexNested6("Either", a, b, c, d) => either(beautifyTypeName(s"$a[$b,$c]"), beautifyTypeName(d))
        case regexNested7("Either", a, b, c) => either(beautifyTypeName(a), beautifyTypeName(s"$b[$c]"))
        case _ => s
      }
    }

    val hierarchyOption = hierarchy match {
      case l if l.length > 0 => Some(l)
      case _ => None
    }

    // name is e.g. "io.viash.config.Config.name", only keep "name"
    // name can also be "__this__"
    // Use the name defined from the class, *unless* the 'nameOverride' annotation is set. Then use the override, unless the name is '__this__'.
    val nameOverride = annotations.collectFirst({case (name, value) if name.endsWith("nameOverride") => value.head})
    val nameFromClass = name.split('.').last
    val name_ = (nameOverride, nameFromClass) match {
      case (Some(_), "__this__") => "__this__"
      case (Some(ann), _) => ann
      case (None, name) => name
    }

    val typeName = (`type`, nameOverride, nameFromClass) match {
      case (_, Some(newTypeName), "__this__") => newTypeName
      case (typeName, _, _) => typeName
    }
    
    val description = annotations.collectFirst({case (name, value) if name.endsWith("description") => value.head})
    val example = annotations.collect({case (name, value) if name.endsWith("example") => value}).map(ExampleSchema(_))
    val exampleWithDescription = annotations.collect({case (name, value) if name.endsWith("exampleWithDescription") => value}).map(ExampleSchema(_))
    val examples = example ::: exampleWithDescription match {
      case l if l.length > 0 => Some(l)
      case _ => None
    }
    val since = annotations.collectFirst({case (name, value) if name.endsWith("since") => value.head})
    val deprecated = annotations.collectFirst({case (name, value) if name.endsWith("deprecated") => value}).map(DeprecatedOrRemovedSchema(_))
    val removed = annotations.collectFirst({case (name, value) if name.endsWith("removed") => value}).map(DeprecatedOrRemovedSchema(_))
    val defaultFromAnnotation = annotations.collectFirst({case (name, value) if name.endsWith("default") => value.head})
    val defaultFromType = Option.when(typeName.startsWith("Option["))("Empty")
    val default = defaultFromAnnotation orElse defaultFromType
    val subclass = annotations.collect{ case (name, value) if name.endsWith("subclass") => value.head } match {
      case l if l.nonEmpty => Some(l.sorted())
      case _ => None
    }
    
    val undocumented = annotations.exists{ case (name, value) => name.endsWith("undocumented")}
    val internalFunctionality = annotations.exists{ case (name, value) => name.endsWith("internalFunctionality")}

    ParameterSchema(name_, typeName, beautifyTypeName(typeName), hierarchyOption, description, examples, since, deprecated, removed, default, subclass, undocumented, internalFunctionality)
  }
}

final case class DeprecatedOrRemovedSchema(
  message: String,
  deprecation: String,
  removal: String,
)

object DeprecatedOrRemovedSchema {
  def apply(l: List[String]): DeprecatedOrRemovedSchema = {
    DeprecatedOrRemovedSchema(l(0), l(1), l(2))
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