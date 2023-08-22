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

import io.viash.functionality.Functionality

case class DockerImageInfo(
  name: String, 
  tag: String = "latest", 
  registry: Option[String] = None,
  organization: Option[String] = None
) {
  override def toString: String = {
    registry.map(_ + "/").getOrElse("") +
    organization.map(_ + "/").getOrElse("") +
      name + ":" + tag
  }

  def toMap: Map[String, String] = {
    val image = organization.map(_ + "/").getOrElse("") + name
    
    registry.map(r => Map("registry" -> r)).getOrElse(Map()) ++
    Map(
      "image" -> image,
      "tag" -> tag
    )
  }
}

object Docker {
  private val TagRegex = "^(.*):(.*)$".r

  def getImageInfo(
    functionality: Option[Functionality] = None,
    name: Option[String] = None,
    registry: Option[String] = None,
    organization: Option[String] = None,
    tag: Option[String] = None,
    namespaceSeparator: String
  ): DockerImageInfo = {

    // If the image name contains a tag, use it
    val (derivedName, derivedTag) = name match {
      case Some(TagRegex(dName, dTag)) => (Some(dName), Some(dTag))
      case _ => (None, None)
    }

    val actualName = {
      derivedName
    } orElse {
      name
    } orElse {
      functionality.flatMap(fun => fun.namespace.map(_ + namespaceSeparator + fun.name))
    } orElse {
      functionality.map(fun => fun.name)
    }

    val actualTag = {
      derivedTag
    } orElse {
      tag
    } orElse {
      functionality.flatMap(fun => fun.version.map(_.toString))
    } orElse {
      Some("latest")
    }

    DockerImageInfo(
      name = actualName.get,
      tag = actualTag.get,
      registry = registry,
      organization = organization
    )
  }

  def listifyOneOrMore(values: Option[Either[String, List[String]]]): Option[String] = {
    values match {
      case None => None
      case Some(Left(s)) => Some(s)
      case Some(Right(list)) if list.isEmpty => Some("[]")
      case Some(Right(list)) => Some(list.mkString("[\"", "\",\"", "\"]"))
    }
  }
}
