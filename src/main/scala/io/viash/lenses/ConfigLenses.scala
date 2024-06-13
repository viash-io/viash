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

import io.viash.config.Config
import monocle.Lens
import monocle.macros.GenLens

import LinksLenses._
import RepositoryLens._

object ConfigLenses {
  val enginesLens = GenLens[Config](_.engines)
  val runnersLens = GenLens[Config](_.runners)

  val nameLens = GenLens[Config](_.name)
  val versionLens = GenLens[Config](_.version)
  val requirementsLens = GenLens[Config](_.requirements)
  val resourcesLens = GenLens[Config](_.resources)
  val testResourcesLens = GenLens[Config](_.test_resources)
  val dependenciesLens = GenLens[Config](_.dependencies)
  val repositoriesLens = GenLens[Config](_.repositories)
  val keywordsLens = GenLens[Config](_.keywords)
  val licenseLens = GenLens[Config](_.license)
  val linksLens = GenLens[Config](_.links)

  val linksRepositoryLens = linksLens ^|-> repositoryLens
  val linksDockerRegistryLens = linksLens ^|-> LinksLenses.dockerRegistryLens
}
