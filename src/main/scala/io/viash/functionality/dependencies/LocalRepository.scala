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

import io.viash.schemas._
import java.nio.file.Paths

@description(
  """Defines a locally present and available repository.
    |This can be used to define components from the same code base as the current component.
    |Alternatively, this can be used to refer to a code repository present on the local hard-drive instead of fetchable remotely, for example during development.
    |""".stripMargin
)
@exampleWithDescription(
  """type: local
    |path: /additional_code/src
    |""".stripMargin,
  "yaml",
  "Refer to a local code repository under `additional_code/src` referenced to the Viash Package Config file."
)
@subclass("local")
case class LocalRepository(
  @description("Defines the repository as a locally present and available repository.")
  `type`: String = "local",
  tag: Option[String] = None,
  path: Option[String] = None,
  localPath: String = ""
) extends LocalRepositoryTrait {

  def copyRepo(
    `type`: String,
    tag: Option[String],
    path: Option[String],
    localPath: String
  ): LocalRepository = {
    copy(`type`, tag, path, localPath)
  }

}
