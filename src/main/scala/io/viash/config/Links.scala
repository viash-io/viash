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

import io.viash.schemas._

@description("Links to external resources related to the component.")
@example(
  """repository: "https://github.com/viash-io/viash"
    |docker_registry: "https://ghcr.io"
    |homepage: "https://viash.io"
    |documentation: "https://viash.io/reference/"
    |issue_tracker: "https://github.com/viash-io/viash/issues"
    |""".stripMargin, "yaml")
@since("Viash 0.9.0")
case class Links(
  @description("Source repository url.")
  @example("""repository: "https://github.com/viash-io/viash"""", "yaml")
  repository: Option[String] = None,

  @description("Docker registry url.")
  @example("""docker_registry: "https://ghcr.io"""", "yaml")
  docker_registry: Option[String] = None,

  @description("Homepage website url.")
  @example("""homepage: "https://viash.io"""", "yaml")
  homepage: Option[String] = None,

  @description("Documentation website url.")
  @example("""documentation: "https://viash.io/reference/"""", "yaml")
  documentation: Option[String] = None,
  
  @description("Issue tracker url.")
  @example("""issue_tracker: "https://github.com/viash-io/viash/issues"""", "yaml")
  issue_tracker: Option[String] = None,
)
