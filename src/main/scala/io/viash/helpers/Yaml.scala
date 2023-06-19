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

import org.yaml.snakeyaml.nodes._
import org.yaml.snakeyaml.{Yaml => Syaml}
import java.io.{StringReader, StringWriter}
import scala.util.Using

object Yaml {
  /**
    * Change valid number +.inf, -.inf, or .nan values and change them to string values
    * 
    * Json does not support these values so we must pass them as strings.
    *
    * @param data Yaml data as text
    * @return Yaml data as text
    */
  def replaceInfinities(data: String): String = {
    Using.Manager { use => 
      val yaml = new Syaml()

      // Convert yaml text to Node tree
      val yamlTree = yaml.compose(new StringReader(data))
      
      // Search for number values of "+.inf" and replace them in place
      recurseReplaceInfinities(yamlTree)

      // Save yaml back to string
      val writer = new StringWriter()
      yaml.serialize(yamlTree, writer)
      writer.toString
    }.get
  }

  private def recurseReplaceInfinities(node: Node): Unit = node match {
    // Traverse maps
    case mapNode: MappingNode => mapNode.getValue().forEach(t => recurseReplaceInfinities(t.getValueNode()))
    // Traverse arrays
    case seqNode: SequenceNode => seqNode.getValue().forEach(n => recurseReplaceInfinities(n))
    // If double and string matches, change type from float to string.
    // The value can stay the same and instead will get escaped during serialization.
    case scalar: ScalarNode if scalar.getTag == Tag.FLOAT =>
      if ("([-+]?\\.(inf|Inf|INF))|\\.(nan|NaN|NAN)".r matches scalar.getValue())
        scalar.setTag(Tag.STR)
    // No changes required
    case _ =>
  }
}
