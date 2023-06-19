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
    // Convert yaml text to Node tree
    val yaml = new Syaml()
    val yamlTree = yaml.compose(new StringReader(data))

    // Search for number values of "+.inf" and replace them in place
    recurseReplaceInfinities(yamlTree)

    // Save yaml back to string
    val writer = new StringWriter()
    yaml.serialize(yamlTree, writer)
    val output = writer.toString
    writer.close()
    output
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
