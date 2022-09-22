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

package io.viash.functionality.dependencies

import java.nio.file.{Path, Paths}
import io.viash.config.Config
import io.viash.config.Config._

case class Dependency(
  name: String,
  `type`: String,
  alias: Option[String],
  tag: Option[String],
  repository: Option[String],
  path: Option[String]
) {
  var linkedRepository: Option[Repository] = None
  def workRepository: Repository = {
    // TODO evaluate required code for 'on the fly' creation of the repo from dependency data
    if (linkedRepository.isDefined)
      linkedRepository.get
    else
      Repository(name = "", `type` = `type`, tag = tag, path = path)
  }

  private var cachePath: Path = Paths.get("")
  private var config: Config = null

  def prepare() {
    // Remote repositories should have been fetched and cached locally (store location where it is cached).

    if (`type` == "local") {
      cachePath = Paths.get("src") // TODO make configurable, using default for namespaces for now
    }
    println(s"cacheLocation: $cachePath")

    // Locate config file
    val configs = readConfigs(cachePath.toString, applyPlatform = false)
    println(s"found configs: $configs")
    


    // Recursively call prepare on those config files.
  }

}

object Dependency {
  def groupByRepository(dependencies: Seq[Dependency]) = {
    dependencies.groupBy(_.workRepository)
  }

}