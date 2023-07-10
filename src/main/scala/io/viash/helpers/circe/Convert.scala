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

package io.viash.helpers.circe

import io.circe.Json
import io.circe.yaml.parser
import scala.util.{Try, Success, Failure}
import io.viash.exceptions.{ConfigYamlException, ConfigParserException}
import io.viash.helpers.circe._
import io.viash.schemas._
import io.circe.Decoder

object Convert {

  private def parsingYamlErrorHandler[C](pathStr: String)(e: Exception): C = {
    // Console.err.println(s"${Console.RED}Error parsing, invalid Yaml structure '${uri}'.${Console.RESET}\nDetails:")
    // throw e
    throw new ConfigYamlException(pathStr, e)
  }
  private def parsingErrorHandler[C](pathStr: String)(e: Exception): C = {
    // Console.err.println(s"${Console.RED}Error parsing '${uri}'.${Console.RESET}\nDetails:")
    // throw e
    throw new ConfigParserException(pathStr, e)
  }

  def textToJson(text: String, pathStr: String) = {
    parser.parse(text).fold(parsingYamlErrorHandler(pathStr), identity)
  }

  def jsonToClass[T](json: Json, pathStr: String)(implicit decoder: Decoder[T]): T = {
    Try(json.as[T]) match {
      case Success(res) => res.fold(parsingErrorHandler(pathStr), identity)
      case Failure(e) => throw new ConfigParserException(pathStr, e)
    }
  }

}
