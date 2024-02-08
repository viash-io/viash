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

import io.viash.config.{AppliedConfig, Config}
import io.viash.functionality.Functionality
import monocle.Lens
import monocle.macros.GenLens

import ConfigLenses._
import FunctionalityLenses._

object AppliedConfigLenses {
  val configLens = GenLens[AppliedConfig](_.config)
  val appliedEnginesLens = GenLens[AppliedConfig](_.engines)
  val appliedRunnerLens = GenLens[AppliedConfig](_.runner)
  
  val functionalityLens = configLens ^|-> ConfigLenses.functionalityLens
  val enginesLens = configLens ^|-> ConfigLenses.enginesLens
  val runnersLens = configLens ^|-> ConfigLenses.runnersLens

  val functionalityNameLens = configLens ^|-> ConfigLenses.composedNameLens
  val functionalityVersionLens = configLens ^|-> ConfigLenses.composedVersionLens
  val functionalityRequirementsLens = configLens ^|-> ConfigLenses.composedRequirementsLens
  val functionalityDependenciesLens = configLens ^|-> ConfigLenses.composedDependenciesLens
  val functionalityRepositoriesLens = configLens ^|-> ConfigLenses.composedRepositoriesLens
}
