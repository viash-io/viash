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

package io.viash.platforms

import io.viash.config.Config
import io.viash.functionality._
import io.viash.functionality.resources._
import io.viash.functionality.arguments._
import io.viash.helpers.{Docker, Bash, DockerImageInfo, Helper}
import io.viash.helpers.circe._
import io.viash.platforms.nextflow._
import io.circe.syntax._
import io.circe.{Printer => JsonPrinter, Json, JsonObject}
import shapeless.syntax.singleton
import io.viash.schemas._
import io.viash.helpers.Escaper

/**
 * A Platform class for generating Nextflow (DSL2) modules.
 */
@description("""Platform for generating Nextflow VDSL3 modules.""".stripMargin)
// todo: add link to guide
@example(
  """platforms:
    |  - type: nextflow
    |    directives:
    |      label: [lowcpu, midmem]
    |""".stripMargin,
  "yaml")
@deprecated("Use 'engines' and 'runners' instead", "0.8.0", "0.9.0")
@subclass("nextflow")
case class NextflowPlatform(
  @description("Every platform can be given a specific id that can later be referred to explicitly when running or building the Viash component.")
  @example("id: foo", "yaml")
  @default("nextflow")
  id: String = "nextflow",

  `type`: String = "nextflow",
  
  // nxf params
  @description(
    """@[Directives](nextflow_directives) are optional settings that affect the execution of the process. These mostly match up with the Nextflow counterparts.  
      |""".stripMargin)
  @example(
    """directives:
      |  container: rocker/r-ver:4.1
      |  label: highcpu
      |  cpus: 4
      |  memory: 16 GB""".stripMargin,
      "yaml")
  @default("Empty")
  directives: NextflowDirectives = NextflowDirectives(),

  @description(
    """@[Automated processing flags](nextflow_auto) which can be toggled on or off:
      |
      || Flag | Description | Default |
      ||---|---------|----|
      || `simplifyInput` | If `true`, an input tuple only containing only a single File (e.g. `["foo", file("in.h5ad")]`) is automatically transformed to a map (i.e. `["foo", [ input: file("in.h5ad") ] ]`). | `true` |
      || `simplifyOutput` | If `true`, an output tuple containing a map with a File (e.g. `["foo", [ output: file("out.h5ad") ] ]`) is automatically transformed to a map (i.e. `["foo", file("out.h5ad")]`). | `true` |
      || `transcript` | If `true`, the module's transcripts from `work/` are automatically published to `params.transcriptDir`. If not defined, `params.publishDir + "/_transcripts"` will be used. Will throw an error if neither are defined. | `false` |
      || `publish` | If `true`, the module's outputs are automatically published to `params.publishDir`.  Will throw an error if `params.publishDir` is not defined. | `false` |
      |
      |""".stripMargin)
  @example(
    """auto:
      |  publish: true""".stripMargin,
      "yaml")
  @default(
    """simplifyInput: true
      |simplifyOutput: true
      |transcript: false
      |publish: false
      |""".stripMargin)
  auto: NextflowAuto = NextflowAuto(),

  @description("Allows tweaking how the @[Nextflow Config](nextflow_config) file is generated.")
  @since("Viash 0.7.4")
  @default("A series of default labels to specify memory and cpu constraints")
  config: NextflowConfig = NextflowConfig(),

  @description("Whether or not to print debug messages.")
  @default("False")
  debug: Boolean = false,

  // TODO: solve differently
  @description("Specifies the Docker platform id to be used to run Nextflow.")
  @default("docker")
  container: String = "docker"
) extends Platform