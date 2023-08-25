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

import io.viash.schemas.description

// TODO: rename to ConfigInfo?

@description("Meta information fields filled in by Viash during build.")
case class Info(
  @description("Path to the config used during build.")
  config: String,
  @description("The platform id used during build.")
  platform: Option[String] = None,
  @description("The executor id used during build.")
  executor: Option[String] = None,
  @description("The engine id used during build.")
  engine: Option[String] = None,
  @description("Folder path to the build artifacts.")
  output: Option[String] = None,
  @description("Output folder with main executable path.")
  executable: Option[String] = None,
  @description("The Viash version that was used to build the component.")
  viash_version: Option[String] = None,
  @description("Git commit hash.")
  git_commit: Option[String] = None,
  @description("Git remote name.")
  git_remote: Option[String] = None,
  @description("Git tag.")
  git_tag: Option[String] = None
)