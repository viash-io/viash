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
import java.io.File

object ViashJsonParser {
  
  def parseJson(path: Option[String] = None): Map[String, Any] = {
    val jsonPath = path.getOrElse {
      sys.env.getOrElse("VIASH_WORK_PARAMS",
        throw new RuntimeException("VIASH_WORK_PARAMS environment variable not set"))
    }
    
    val file = new File(jsonPath)
    if (!file.exists()) {
      throw new RuntimeException(s"Parameters file not found: $jsonPath")
    }
    
    val jsonText = Source.fromFile(file).mkString
    parse(jsonText).asInstanceOf[Map[String, Any]]
  }
  
  private def parse(json: String): Any = {
    var pos = 0
    
    def skipWhitespace(): Unit = {
      while (pos < json.length && json(pos).isWhitespace) pos += 1
    }
    
    def peek: Char = {
      skipWhitespace()
      if (pos < json.length) json(pos) else throw new RuntimeException("Unexpected end of JSON")
    }
    
    def consume(expected: Char): Unit = {
      skipWhitespace()
      if (pos >= json.length || json(pos) != expected) {
        throw new RuntimeException(s"Expected '$expected' at position $pos")
      }
      pos += 1
    }
    
    def parseValue(): Any = {
      peek match {
        case '"' => parseString()
        case '{' => parseObject()
        case '[' => parseArray()
        case 't' | 'f' => parseBoolean()
        case 'n' => parseNull()
        case c if c == '-' || c.isDigit => parseNumber()
        case c => throw new RuntimeException(s"Unexpected character '$c' at position $pos")
      }
    }
    
    def parseString(): String = {
      consume('"')
      val sb = new StringBuilder
      while (pos < json.length && json(pos) != '"') {
        if (json(pos) == '\\') {
          pos += 1
          if (pos >= json.length) throw new RuntimeException("Unterminated string escape")
          json(pos) match {
            case 'n' => sb.append('\n')
            case 't' => sb.append('\t')
            case 'r' => sb.append('\r')
            case 'b' => sb.append('\b')
            case 'f' => sb.append('\f')
            case '\\' => sb.append('\\')
            case '"' => sb.append('"')
            case '/' => sb.append('/')
            case 'u' =>
              if (pos + 4 >= json.length) throw new RuntimeException("Invalid unicode escape")
              val hex = json.substring(pos + 1, pos + 5)
              sb.append(Integer.parseInt(hex, 16).toChar)
              pos += 4
            case c => sb.append(c)
          }
        } else {
          sb.append(json(pos))
        }
        pos += 1
      }
      consume('"')
      sb.toString
    }
    
    def parseNumber(): Any = {
      val start = pos
      if (json(pos) == '-') pos += 1
      while (pos < json.length && json(pos).isDigit) pos += 1
      
      val hasDecimal = pos < json.length && json(pos) == '.'
      if (hasDecimal) {
        pos += 1
        while (pos < json.length && json(pos).isDigit) pos += 1
      }
      
      val hasExponent = pos < json.length && (json(pos) == 'e' || json(pos) == 'E')
      if (hasExponent) {
        pos += 1
        if (pos < json.length && (json(pos) == '+' || json(pos) == '-')) pos += 1
        while (pos < json.length && json(pos).isDigit) pos += 1
      }
      
      val numStr = json.substring(start, pos)
      if (hasDecimal || hasExponent) {
        numStr.toDouble
      } else {
        val n = BigInt(numStr)
        if (n.isValidInt) n.toInt
        else if (n.isValidLong) n.toLong
        else n.toDouble
      }
    }
    
    def parseBoolean(): Boolean = {
      if (json.substring(pos).startsWith("true")) {
        pos += 4
        true
      } else if (json.substring(pos).startsWith("false")) {
        pos += 5
        false
      } else {
        throw new RuntimeException(s"Invalid boolean at position $pos")
      }
    }
    
    def parseNull(): Null = {
      if (json.substring(pos).startsWith("null")) {
        pos += 4
        null
      } else {
        throw new RuntimeException(s"Invalid null at position $pos")
      }
    }
    
    def parseArray(): List[Any] = {
      consume('[')
      if (peek == ']') {
        consume(']')
        return Nil
      }
      
      val items = scala.collection.mutable.ListBuffer[Any]()
      items += parseValue()
      while (peek == ',') {
        consume(',')
        items += parseValue()
      }
      consume(']')
      items.toList
    }
    
    def parseObject(): Map[String, Any] = {
      consume('{')
      if (peek == '}') {
        consume('}')
        return Map.empty
      }
      
      val entries = scala.collection.mutable.Map[String, Any]()
      def parseEntry(): Unit = {
        val key = parseString()
        consume(':')
        entries(key) = parseValue()
      }
      
      parseEntry()
      while (peek == ',') {
        consume(',')
        parseEntry()
      }
      consume('}')
      entries.toMap
    }
    
    parseValue()
  }
}
