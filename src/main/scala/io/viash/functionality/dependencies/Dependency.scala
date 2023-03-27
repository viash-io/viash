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
import io.viash.schemas._

@description(
  """Specifies a Viash component (script or executable) that should be made available for the code defined in the functionality.
    |The dependency components are collected and copied to the output folder during the Viash build step.
    |""".stripMargin)
case class Dependency(
  @description("The full name of the dependency component. This should include the namespace.")
  @example("name: \"my_namespace\"component", "yaml")
  name: String,
  
  @description(
    """Specifies the location where the dependency component can be found.
      |This must either be a full definition of the repository or the name of a repository refenced as it is defined under functionality.repositories.
      |Additionally, the full definition can be specified as a single string where all parameters such as repository type, url, branch or tag are specified.
      |""".stripMargin)
  repository: Either[String, Repository] = Right(LocalRepository()),

  // internal stuff
  @internalFunctionality
  @description("Location of the config of this dependency component.")
  foundConfigPath: Option[String] = None,

  @internalFunctionality
  @description("Content of the config of this dependency component.")
  workConfig: Option[Config] = None,
  configInfo: Map[String, String] = Map.empty,

  @internalFunctionality
  @description("Location of the dependency component artifacts are written ready to be used.")
  writtenPath: Option[String] = None,
) {
  def workRepository: Option[Repository] = repository.toOption
}
