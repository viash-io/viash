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
import io.circe.yaml.{Printer => YamlPrinter}
import io.circe.syntax.EncoderOps
import io.viash.helpers.circe._
import java.nio.file.{Path, Paths, Files}
import io.viash.schemas._
import io.circe.Json

object ViashExport extends Logging {
  def exportCLISchema(output: Option[Path], format: String): Unit = {
    val cli = new CLIConf(Nil)
    val data = cli.getRegisteredCommands().asJson
    val str = data.toFormattedString(format)
    if (output.isDefined) {
      Files.write(output.get, str.getBytes())
    } else {
      infoOut(str)
    }
  }

  def exportAutocomplete(output: Option[Path], format: String): Unit = {
    val cli = new CLIConf(Nil)
    val str = 
      format match {
        case "bash" => AutoCompleteBash.generate(cli)
        case "zsh" => AutoCompleteZsh.generate(cli)
        case _ => throw new IllegalArgumentException("'format' must be either 'bash' or 'zsh'.")
      }
    if (output.isDefined) {
      Files.write(output.get, str.getBytes())
    } else {
      infoOut(str)
    }
  }

  def exportConfigSchema(output: Option[Path], format: String): Unit = {
    val data = CollectedSchemas.getJson
    val str = data.toFormattedString(format)
    if (output.isDefined) {
      Files.write(output.get, str.getBytes())
    } else {
      infoOut(str)
    }
  }


  def exportJsonSchema(output: Option[Path], format: String, strict: Boolean, minimal: Boolean): Unit = {
    val data = JsonSchema.getJsonSchema(strict, minimal)
    val str = data.toFormattedString(format)
    if (output.isDefined) {
      Files.write(output.get, str.getBytes())
    } else {
      infoOut(str)
    }
  }

  def exportResource(input: String, output: Option[Path]): Unit = {
    val input2 = if (input.startsWith("platforms/")) {
      warn("WARNING: The 'platforms/' prefix is deprecated. Please use 'runners/' instead.")
      input.replaceFirst("platforms/", "runners/")
    } else {
      input
    }
    val pth = getClass.getResource(s"/io/viash/$input2")
    val str = IO.read(pth.toURI())
    if (output.isDefined) {
      Files.write(output.get, str.getBytes())
    } else {
      infoOut(str)
    }
  }
}
