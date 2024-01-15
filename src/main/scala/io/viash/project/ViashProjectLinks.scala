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

package io.viash.project

import io.viash.schemas._

@description("Links to external resources related to the project.")
@since("Viash 0.9.0")
case class ViashProjectLinks(
  @description("Source repository url (can be used to define the target_image_source in Docker images).")
  source: Option[String] = None,
  @description("Docker registry url (can be used to define the target registry in Docker images).")
  docker_registry: Option[String] = None,
  @description("Documentation website.")
  homepage: Option[String] = None,
  @description("Issue tracker.")
  issue_tracker: Option[String] = None,
)
