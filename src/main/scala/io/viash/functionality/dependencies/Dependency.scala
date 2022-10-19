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

case class Dependency(
  name: String,
  repository: Either[String, Repository] = Right(LocalRepository()),

  // internal stuff
  foundConfigPath: String = "",
  workConfig: Option[Config] = None
) {
  def workRepository: Option[Repository] = repository.toOption
}

object Dependency {
  def groupByRepository(dependencies: Seq[Dependency]) = {
    dependencies.groupBy(_.workRepository)
  }

}