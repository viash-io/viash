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

package com.dataintuitive.viash.config

case class Info(
  config: String,
  platform: Option[String] = None,
  output: Option[String] = None,
  executable: Option[String] = None,
  viash_version: Option[String] = None,
  git_commit: Option[String] = None,
  git_remote: Option[String] = None,
  git_tag: Option[String] = None
) {
  def consoleString: String = {
    val missing = "<NA>"
    s"""viash version:      ${viash_version.getOrElse(missing)}
       |config:             ${config}
       |platform:           ${platform.getOrElse(missing)}
       |executable:         ${executable.getOrElse(missing)}
       |output:             ${output.getOrElse(missing)}
       |remote git repo:    ${git_remote.getOrElse(missing)}""".stripMargin
  }

  def parent_path: String = {
    val regex = "[^/]*$".r
    regex.replaceFirstIn(config, "")
  }
}