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

package io.viash.exceptions

abstract class AbstractConfigException extends Exception {
  val uri: String
  val e: Throwable
  val innerMessage: String
  
  override def getMessage(): String = e.getMessage()
}


case class ConfigParserException(uri: String, e: Throwable) extends AbstractConfigException {
  val innerMessage: String = "invalid Viash Config content"
}

case class ConfigYamlException(uri: String, e: Throwable) extends AbstractConfigException {
  val innerMessage: String = "invalid Yaml structure"
}

case class ConfigParserSubTypeException(tpe: String, validTypes: List[String], json: String) extends Exception {

  private val validTypesStr = validTypes.dropRight(1).mkString("'", "', '", "'") + s", and '${validTypes.last}'"
  override def getMessage(): String = s"Type '$tpe' is not recognised. Valid types are $validTypesStr.\n$json"
}

case class ConfigParserValidationException(tpe: String, json: String, hint: Option[Seq[String]] = None) extends Exception {
  val shortType = tpe.split("\\.").last
  val hintString = hint match {
    case Some(h) if h.length > 1 => s"Likely problematic fields: ${h.mkString(", ")}\n"
    case Some(h) if h.length == 1 => s"Likely problematic field: ${h.head}\n"
    case _ => ""
  }
  override def getMessage(): String = s"Invalid data fields for $shortType.\n$hintString$json"
}

case class ConfigParserMergeException(uri: String, innerMessage: String, json: String) extends AbstractConfigException {
  val e: Throwable = null
  override def getMessage(): String = json
}