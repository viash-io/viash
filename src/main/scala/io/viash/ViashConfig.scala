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

import java.nio.file.{Files, Paths}
import scala.sys.process.Process

import io.circe.syntax.EncoderOps

import io.viash.config.Config
import io.viash.config.arguments._
import io.viash.helpers.{IO, Logging}
import io.viash.helpers.circe._
import io.viash.helpers.data_structures._
import io.viash.runners.DebugRunner
import io.viash.config.ConfigMeta
import io.viash.exceptions.ExitException
import io.viash.runners.Runner

object ViashConfig extends Logging{

  /**
   * Augment required arguments with placeholder examples if they don't have examples or defaults.
   * This ensures consistency across languages when generating config inject code.
   */
  private def addPlaceholderExamples(config: Config): Config = {
    val newGrps = config.argument_groups.map(grp =>
      val newArgs = grp.arguments.map { arg =>
        // Only add placeholder if required and missing both example and default
        if (arg.required && arg.example.toList.isEmpty && arg.default.toList.isEmpty) {
          val placeholder = arg match {
            case a: StringArgument => a.copy(example = OneOrMore("placeholder"))
            case a: FileArgument => a.copy(example = OneOrMore(Paths.get("path/to/file")))
            case a: IntegerArgument => a.copy(example = OneOrMore(123))
            case a: LongArgument => a.copy(example = OneOrMore(123456L))
            case a: DoubleArgument => a.copy(example = OneOrMore(12.34))
            case a: BooleanArgument => a.copy(example = OneOrMore(true))
            // BooleanTrueArgument and BooleanFalseArgument automatically have a default
            case other => other
          }
          placeholder.asInstanceOf[Argument[_]]
        } else {
          arg
        }
      }
      grp.copy(arguments = newArgs)
    )

    config.copy(argument_groups = newGrps)
  }

  def view(config: Config, format: String): Unit = {
    val json = ConfigMeta.configToCleanJson(config)
    infoOut(json.toFormattedString(format))
  }

  def viewMany(configs: List[Config], format: String): Unit = {
    val jsons = configs.map(c => ConfigMeta.configToCleanJson(c))
    infoOut(jsons.asJson.toFormattedString(format))
  }

  def inject(config: Config, force: Boolean = false): Unit = {
    // check if config has a main script
    if (config.mainScript.isEmpty) {
      infoOut("Could not find a main script in the Viash config.")
      throw new ExitException(1)
    }
    val mainScript = config.mainScript.get
    
    // check if we can read code
    if (mainScript.readSome.isEmpty) {
      infoOut("Could not read main script in the Viash config.")
      throw new ExitException(1)
    }
    // check if main script has a path
    if (mainScript.uri.isEmpty) {
      infoOut("Main script should have a path.")
      throw new ExitException(1)
    }
    val uri = mainScript.uri.get

    // check if main script is a local file
    if (uri.getScheme != "file") {
      infoOut("Config inject only works for local Viash configs.")
      throw new ExitException(1)
    }
    val path = Paths.get(uri.getPath())

    // Augment config with placeholder examples for required arguments
    val augmentedConfig = addPlaceholderExamples(config)
    
    // Generate args, meta, and deps maps
    val argsMetaAndDeps = augmentedConfig.getArgumentLikesGroupedByDest(
      includeMeta = true,
      includeDependencies = true,
      filterInputs = true
    )

    // Generate injected code
    val script = mainScript.asInstanceOf[io.viash.config.resources.Script]
    val newCode = script.readWithConfigInject(argsMetaAndDeps, augmentedConfig)

    // Ask for confirmation unless --force is specified
    if (!force) {
      infoOut(s"About to modify: $path")
      infoOut("This will inject parameter definitions into the script between VIASH START and VIASH END markers.")
      infoOut("Do you want to continue? (y/N): ")
      val response = scala.io.StdIn.readLine()
      if (response.toLowerCase != "y" && response.toLowerCase != "yes") {
        infoOut("Cancelled.")
        return
      }
    }

    // Write the modified script back to the file
    IO.write(newCode, path)
    infoOut(s"Successfully injected Viash header into: $path")
  }


}
