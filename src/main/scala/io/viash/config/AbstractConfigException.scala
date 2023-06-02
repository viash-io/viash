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

package io.viash.config

abstract class AbstractConfigException extends Exception {
  val uri: String
  val e: Exception
  val innerMessage: String
  
  override def getMessage(): String = e.getMessage()
}


case class ConfigParserException(uri: String, e: Exception) extends AbstractConfigException {
  val innerMessage: String = "invalid Viash Config content"
}

case class ConfigYamlException(uri: String, e: Exception) extends AbstractConfigException {
  val innerMessage: String = "invalid Yaml structure"
}

class ConfigParserSubTypeException(name: String) extends Exception