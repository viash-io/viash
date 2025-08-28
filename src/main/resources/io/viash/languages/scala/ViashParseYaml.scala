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

import scala.io.Source
import scala.util.matching.Regex
import scala.collection.mutable

object ViashYamlParser {
  
  /**
   * Parse simple YAML into a Scala Map.
   * 
   * This function reads YAML content and converts it into a Scala Map.
   * Arrays are converted to Scala Lists.
   * 
   * @param yamlContent YAML content as string. If None, reads from stdin.
   * @return Map containing the parsed YAML data
   * 
   * The YAML format expected is simple:
   *   key: value
   *   array_key:
   *     - item1
   *     - item2
   */
  def parseYaml(yamlContent: Option[String] = None): Map[String, Any] = {
    val content = yamlContent.getOrElse(Source.stdin.mkString)
    
    val result = mutable.Map[String, Any]()
    val lines = content.trim.split('\n')
    var i = 0
    var currentSection: Option[String] = None
    
    while (i < lines.length) {
      val line = lines(i).replaceAll("\\s+$", "") // trim right
      
      // Skip empty lines and comments
      if (line.trim.isEmpty || line.trim.startsWith("#")) {
        i += 1
      } else {
        // Check for top-level sections (section name followed by colon)
        val sectionPattern: Regex = """^([a-zA-Z_][a-zA-Z0-9_]*):\s*$""".r
        line match {
          case sectionPattern(sectionName) =>
            currentSection = Some(sectionName)
            result(sectionName) = mutable.Map[String, Any]()
            i += 1
          case _ =>
            // Check for key-value pairs
            val keyValuePattern: Regex = """^(\s*)([^:]+):\s*(.*)""".r
            line match {
              case keyValuePattern(indent, key, value) =>
                val cleanKey = key.trim
                val cleanValue = value.trim
                
                if (cleanValue.isEmpty) {
                  // This might be the start of an array
                  // Look ahead to see if next lines are array items
                  var j = i + 1
                  val arrayItems = mutable.ListBuffer[Any]()
                  
                  var continue = true
                  while (j < lines.length && continue) {
                    val nextLine = lines(j).replaceAll("\\s+$", "")
                    
                    if (nextLine.trim.isEmpty) {
                      j += 1
                    } else {
                      // Check if it's an array item
                      val arrayPattern: Regex = """^(\s*)-\s*(.*)""".r
                      nextLine match {
                        case arrayPattern(itemIndent, itemValue) =>
                          // Make sure it's indented more than the key
                          if (itemIndent.length > indent.length) {
                            arrayItems += parseValue(itemValue.trim)
                            j += 1
                          } else {
                            continue = false
                          }
                        case _ =>
                          continue = false
                      }
                    }
                  }
                  
                  if (arrayItems.nonEmpty) {
                    // Store in current section or root
                    val parsedValue = arrayItems.toList
                    currentSection match {
                      case Some(section) =>
                        val sectionMap = result(section).asInstanceOf[mutable.Map[String, Any]]
                        sectionMap(cleanKey) = parsedValue
                      case None =>
                        result(cleanKey) = parsedValue
                    }
                    i = j
                  } else {
                    // Empty value - store in current section or root
                    currentSection match {
                      case Some(section) =>
                        val sectionMap = result(section).asInstanceOf[mutable.Map[String, Any]]
                        sectionMap(cleanKey) = None
                      case None =>
                        result(cleanKey) = None
                    }
                    i += 1
                  }
                } else {
                  // Regular key-value pair - store in current section or root
                  val parsedValue = parseValue(cleanValue)
                  currentSection match {
                    case Some(section) =>
                      val sectionMap = result(section).asInstanceOf[mutable.Map[String, Any]]
                      sectionMap(cleanKey) = parsedValue
                    case None =>
                      result(cleanKey) = parsedValue
                  }
                  i += 1
                }
              case _ =>
                i += 1
            }
        }
      }
    }
    
    // Convert mutable maps to immutable
    result.map {
      case (key, value: mutable.Map[String, Any]) => key -> value.toMap
      case (key, value) => key -> value
    }.toMap
  }
  
  /**
   * Parse a YAML value into appropriate Scala type.
   * @param value Value to parse
   * @return Parsed value with appropriate Scala type
   */
  private def parseValue(value: String): Any = {
    value match {
      case "null" => None
      case "true" => true
      case "false" => false
      case quoted if quoted.matches("^\"(.*)\"$") =>
        // Double quoted string - unescape
        var unquoted = quoted.substring(1, quoted.length - 1)
        unquoted = unquoted.replace("\\\"", "\"")
        unquoted = unquoted.replace("\\n", "\n")
        unquoted = unquoted.replace("\\\\", "\\")
        unquoted
      case quoted if quoted.matches("^'(.*)'$") =>
        // Single quoted string - unescape
        var unquoted = quoted.substring(1, quoted.length - 1)
        unquoted = unquoted.replace("\\'", "'")
        unquoted = unquoted.replace("\\n", "\n")
        unquoted = unquoted.replace("\\\\", "\\")
        unquoted
      case integer if integer.matches("^-?\\d+$") =>
        // Integer
        try {
          integer.toInt
        } catch {
          case _: NumberFormatException =>
            try {
              integer.toLong
            } catch {
              case _: NumberFormatException => integer
            }
        }
      case double if double.matches("^-?\\d*\\.\\d+$") =>
        // Double
        try {
          double.toDouble
        } catch {
          case _: NumberFormatException => double
        }
      case _ =>
        // Unquoted string
        value
    }
  }
}
