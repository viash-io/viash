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

package com.dataintuitive.viash

import com.dataintuitive.viash.config.Config
import java.nio.file.{Paths}
import io.circe.syntax.EncoderOps
import io.circe.yaml.Printer

object ViashConfig {
  private val printer = Printer(
    preserveOrder = true,
    dropNullKeys = true,
    mappingStyle = Printer.FlowStyle.Block,
    splitLines = true,
    stringStyle = Printer.StringStyle.DoubleQuoted
  )

  def view(config: Config) {
    val json = config.asJson
    val configYamlStr = printer.pretty(json)
    println(configYamlStr)
  }

  def viewMany(configs: List[Config]) {
    val json = configs.asJson
    val configYamlStr = printer.pretty(json)
    println(configYamlStr)
  }

  def inject(config: Config) {
    val fun = config.functionality

    // check if config has a main script
    if (fun.mainScript.isEmpty) {
      println("Could not find a main script in the Viash config.")
      System.exit(1)
    }
    val mainScript = fun.mainScript.get

    // check if we can read code
    val maybeCode = mainScript.readWithPlaceholder(fun)
    if (maybeCode.isEmpty) {
      println("Could not read main script in the Viash config.")
      System.exit(1)
    }
    val code = maybeCode.get

    // check if main script has a path
    if (mainScript.uri.isEmpty) {
      println("Main script should have a path.")
      System.exit(1)
    }
    val uri = mainScript.uri.get

    // check if main script is a local file
    if (uri.getScheme != "file") {
      println("Config inject only works for local Viash configs.")
      System.exit(1)
    }
    val path = Paths.get(uri.getPath())

    // create new script
    val newScript = mainScript.copyResource(
      text = Some(code),
      dest = Some(mainScript.filename),
      path = None
    )

    // write to destination
    newScript.write(path, overwrite = true)
  }
}
