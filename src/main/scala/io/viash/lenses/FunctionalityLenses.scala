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

package io.viash.lenses

import io.viash.functionality.Functionality

import monocle.Lens
import monocle.macros.GenLens

import LinksLenses._

object FunctionalityLenses {
  val nameLens = GenLens[Functionality](_.name)
  val versionLens = GenLens[Functionality](_.version)
  val requirementsLens = GenLens[Functionality](_.requirements)
  val resourcesLens = GenLens[Functionality](_.resources)
  val testResourcesLens = GenLens[Functionality](_.test_resources)
  val dependenciesLens = GenLens[Functionality](_.dependencies)
  val repositoriesLens = GenLens[Functionality](_.repositories)
  val keywordsLens = GenLens[Functionality](_.keywords)
  val licenseLens = GenLens[Functionality](_.license)
  val organizationLens = GenLens[Functionality](_.organization)
  val linksLens = GenLens[Functionality](_.links)

  val composedLinksRepositoryLens = linksLens ^|-> repositoryLens
  val composedLinksDockerRegistryLens = linksLens ^|-> dockerRegistryLens
}
