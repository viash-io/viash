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

package io.viash.platforms.nextflow

import io.viash.schemas._

@description("Automated processing flags which can be toggled on or off.")
case class NextflowAuto(
  @description(
    """If `true`, an input tuple only containing only a single File (e.g. `["foo", file("in.h5ad")]`) is automatically transformed to a map (i.e. `["foo", [ input: file("in.h5ad") ] ]`).
      |
      |Default: `true`.
      |""".stripMargin)
  @default("True")
  simplifyInput: Boolean = true,

  @description(
    """If `true`, an output tuple containing a map with a File (e.g. `["foo", [ output: file("out.h5ad") ] ]`) is automatically transformed to a map (i.e. `["foo", file("out.h5ad")]`).
      |
      |Default: `false`.
      |""".stripMargin)
  @default("False")
  simplifyOutput: Boolean = false,

  @description(
    """If `true`, the module's transcripts from `work/` are automatically published to `params.transcriptDir`.
      |If not defined, `params.publishDir + "/_transcripts"` will be used.
      |Will throw an error if neither are defined.
      |
      |Default: `false`.
      |""".stripMargin)
  @default("False")
  transcript: Boolean = false,

  @description(
    """If `true`, the module's outputs are automatically published to `params.publishDir`.
      |If equal to `"state"`, also a `.state.yaml` file will be published in the publish dir.
      |Will throw an error if `params.publishDir` is not defined.
      |
      |Default: `false`.
      |""".stripMargin)
  @default("False")
  publish: Either[Boolean, String] = Left(false)
)