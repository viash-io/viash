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

package io.viash.config.dependencies

import io.viash.schemas._

@description("Specifies a repository where dependency components can be found.")
@subclass("LocalRepositoryWithName")
@subclass("GitRepositoryWithName")
@subclass("GithubRepositoryWithName")
@subclass("ViashhubRepositoryWithName")
abstract class RepositoryWithName extends Repository {
  @description("The identifier used to refer to this repository from dependencies.")
  val name: String

  def withoutName: Repository
}
