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
  
  def parseJson(jsonPath: Option[String] = None): Map[String, Any] = {
    val path = jsonPath.getOrElse {
      sys.env.getOrElse("VIASH_WORK_PARAMS", 
        throw new RuntimeException("VIASH_WORK_PARAMS environment variable not set"))
    }
    
    val file = new File(path)
    if (!file.exists()) {
      throw new RuntimeException(s"Parameters file not found: $path")
    }
    
    val jsonText = Source.fromFile(file).mkString
    parseJsonString(jsonText).asInstanceOf[Map[String, Any]]
  }
  
  private def parseJsonString(json: String): Any = {
    var pos = 0
    
    def skipWhitespace(): Unit = {
      while (pos < json.length && json(pos).isWhitespace) pos += 1
    }
    
    def parseValue(): Any = {
      skipWhitespace()
      if (pos >= json.length) throw new RuntimeException("Unexpected end of JSON")
      
      json(pos) match {
        case '"' => parseString()
        case '{' => parseObject()
        case '[' => parseArray()
        case 't' | 'f' => parseBoolean()
        case 'n' => parseNull()
        case c if c == '-' || c.isDigit => parseNumber()
        case c => throw new RuntimeException(s"Unexpected character: $c")
      }
    }
    
    def parseString(): String = {
      pos += 1
      val sb = new StringBuilder
      while (pos < json.length && json(pos) != '"') {
        if (json(pos) == '\\') {
          pos += 1
          if (pos >= json.length) throw new RuntimeException("Unterminated string")
          json(pos) match {
            case 'n' => sb.append('\n')
            case 't' => sb.append('\t')
            case 'r' => sb.append('\r')
            case '\\' => sb.append('\\')
            case '"' => sb.append('"')
            case '/' => sb.append('/')
            case c => sb.append(c)
          }
        } else {
          sb.append(json(pos))
        }
        pos += 1
      }
      if (pos >= json.length) throw new RuntimeException("Unterminated string")
      pos += 1
      sb.toString
    }
    
    def parseNumber(): Any = {
      val start = pos
      if (json(pos) == '-') pos += 1
      while (pos < json.length && json(pos).isDigit) pos += 1
      
      var isDouble = false
      if (pos < json.length && json(pos) == '.') {
        isDouble = true
        pos += 1
        while (pos < json.length && json(pos).isDigit) pos += 1
      }
      
      if (pos < json.length && (json(pos) == 'e' || json(pos) == 'E')) {
        isDouble = true
        pos += 1
        if (pos < json.length && (json(pos) == '+' || json(pos) == '-')) pos += 1
        while (pos < json.length && json(pos).isDigit) pos += 1
      }
      
      val numStr = json.substring(start, pos)
      if (isDouble) numStr.toDouble else numStr.toInt
    }
    
    def parseBoolean(): Boolean = {
      if (pos + 4 <= json.length && json.substring(pos, pos + 4) == "true") {
        pos += 4
        true
      } else if (pos + 5 <= json.length && json.substring(pos, pos + 5) == "false") {
        pos += 5
        false
      } else {
        throw new RuntimeException("Invalid boolean")
      }
    }
    
    def parseNull(): Null = {
      if (pos + 4 <= json.length && json.substring(pos, pos + 4) == "null") {
        pos += 4
        null
      } else {
        throw new RuntimeException("Invalid null")
      }
    }
    
    def parseArray(): List[Any] = {
      pos += 1
      skipWhitespace()
      
      if (pos < json.length && json(pos) == ']') {
        pos += 1
        return List.empty
      }
      
      val result = scala.collection.mutable.ListBuffer[Any]()
      while (true) {
        result += parseValue()
        skipWhitespace()
        
        if (pos >= json.length) throw new RuntimeException("Unterminated array")
        
        if (json(pos) == ']') {
          pos += 1
          return result.toList
        } else if (json(pos) == ',') {
          pos += 1
          skipWhitespace()
        } else {
          throw new RuntimeException(s"Expected ',' or ']'")
        }
      }
      List.empty
    }
    
    def parseObject(): Map[String, Any] = {
      pos += 1
      skipWhitespace()
      
      if (pos < json.length && json(pos) == '}') {
        pos += 1
        return Map.empty
      }
      
      val result = scala.collection.mutable.Map[String, Any]()
      while (true) {
        skipWhitespace()
        
        if (pos >= json.length || json(pos) != '"') {
          throw new RuntimeException("Expected string key")
        }
        val key = parseString()
        
        skipWhitespace()
        if (pos >= json.length || json(pos) != ':') {
          throw new RuntimeException("Expected ':'")
        }
        pos += 1
        
        val value = parseValue()
        result(key) = value
        
        skipWhitespace()
        if (pos >= json.length) throw new RuntimeException("Unterminated object")
        
        if (json(pos) == '}') {
          pos += 1
          return result.toMap
        } else if (json(pos) == ',') {
          pos += 1
        } else {
          throw new RuntimeException(s"Expected ',' or '}'")
        }
      }
      Map.empty
    }
    
    parseValue()
  }
}
