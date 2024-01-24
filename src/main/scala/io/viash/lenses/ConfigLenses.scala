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
import io.viash.functionality.Functionality
import monocle.Lens
import monocle.macros.GenLens

import FunctionalityLenses._


object ConfigLenses {
  val functionalityLens = GenLens[Config](_.functionality)
  val enginesLens = GenLens[Config](_.engines)
  val runnersLens = GenLens[Config](_.runners)

  val composedNameLens = functionalityLens ^|-> nameLens
  val composedVersionLens = functionalityLens ^|-> versionLens
  val composedRequirementsLens = functionalityLens ^|-> requirementsLens
  val composedResourcesLens = functionalityLens ^|-> resourcesLens
  val composedTestResourcesLens = functionalityLens ^|-> testResourcesLens
  val composedDependenciesLens = functionalityLens ^|-> dependenciesLens
  val composedRepositoriesLens = functionalityLens ^|-> repositoriesLens
  val composedKeywordsLens = functionalityLens ^|-> keywordsLens
  val composedLicenseLens = functionalityLens ^|-> licenseLens
  val composedOrganizationLens = functionalityLens ^|-> organizationLens
}
