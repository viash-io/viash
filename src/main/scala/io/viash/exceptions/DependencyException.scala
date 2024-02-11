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

package io.viash.exceptions

import java.nio.file.Path
import io.viash.functionality.dependencies.Dependency
import io.viash.functionality.dependencies.Repository

abstract class AbstractDependencyException extends Exception

case class MissingBuildYamlException(sourcePath: Path, dependency: Dependency) extends AbstractDependencyException {
  override def getMessage() = s"Could not find '.build.yaml' when traversing up from '${sourcePath.toString()}' for '${dependency.name}'"
}

case class CheckoutException(repo: Repository) extends AbstractDependencyException {
  override def getMessage(): String = s"Could not checkout remote repository of type ${repo.`type`}"
}

case class MissingDependencyException(dependencies: List[Dependency]) extends AbstractDependencyException {
  override def getMessage(): String = 
    if (dependencies.size == 1) s"Could not find dependency '${dependencies.head.name}'"
    else s"Could not find dependencies '${dependencies.map(_.name).mkString("', '")}'"
}