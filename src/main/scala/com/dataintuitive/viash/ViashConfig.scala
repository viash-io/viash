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
import com.dataintuitive.viash.helpers.IO

import java.nio.file.{Files, Paths}
import io.circe.syntax.EncoderOps
import io.circe.yaml.Printer

import scala.sys.process.Process
import com.dataintuitive.viash.platforms.DebugPlatform

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
    // check if we can read code
    if (fun.mainScript.get.read.isEmpty) {
      println("Could not read main script in the Viash config.")
      System.exit(1)
    }
    // check if main script has a path
    if (fun.mainScript.get.uri.isEmpty) {
      println("Main script should have a path.")
      System.exit(1)
    }
    val uri = fun.mainScript.get.uri.get

    // check if main script is a local file
    if (uri.getScheme != "file") {
      println("Config inject only works for local Viash configs.")
      System.exit(1)
    }
    val path = Paths.get(uri.getPath())

    // debugFun
    val debugPlatform = DebugPlatform(path = uri.getPath())
    val debugFun = debugPlatform.modifyFunctionality(fun)

    // create temporary directory
    val dir = IO.makeTemp("viash_inject_" + config.functionality.name)

    // build regular executable
    Files.createDirectories(dir)
    IO.writeResources(debugFun.resources, dir)

    // run command, collect output
    val executable = Paths.get(dir.toString, fun.name).toString
    val exitValue = Process(Seq(executable), cwd = dir.toFile).!

    // TODO: remove tempdir
  }


}
