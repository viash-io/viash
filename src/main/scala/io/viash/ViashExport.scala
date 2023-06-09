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

package io.viash

import helpers._
import cli._
import io.circe.{Printer => JsonPrinter}
import io.circe.syntax.EncoderOps
import io.viash.helpers.circe._
import java.nio.file.{Path, Paths, Files}
import io.viash.schemas.{CollectedSchemas, JsonSchema}

object ViashExport {
  private val jsonPrinter = JsonPrinter.spaces2.copy(dropNullValues = true)

  def exportCLISchema(output: Option[Path]): Unit = {
    val cli = new CLIConf(Nil)
    val data = cli.getRegisteredCommands
    val str = jsonPrinter.print(data.asJson)
    if (output.isDefined) {
      Files.write(output.get, str.getBytes())
    } else {
      println(str)
    }
  }

  def exportConfigSchema(output: Option[Path]): Unit = {
    val data = CollectedSchemas.getJson
    val str = jsonPrinter.print(data.asJson)
    if (output.isDefined) {
      Files.write(output.get, str.getBytes())
    } else {
      println(str)
    }
  }

  def exportJsonSchema(output: Option[Path]): Unit = {
    val data = JsonSchema.getJsonSchema
    val str = jsonPrinter.print(data.asJson)
    if (output.isDefined) {
      Files.write(output.get, str.getBytes())
    } else {
      println(str)
    }
  }

  def exportResource(input: String, output: Option[Path]): Unit = {
    val pth = getClass.getResource(s"/io/viash/$input")
    val str = IO.read(pth.toURI())
    if (output.isDefined) {
      Files.write(output.get, str.getBytes())
    } else {
      println(str)
    }
  }
}
