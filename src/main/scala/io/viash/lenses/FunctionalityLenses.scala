package io.viash.lenses

import io.viash.functionality.Functionality

import monocle.Lens
import monocle.macros.GenLens

object FunctionalityLenses {
  val dependenciesLens = GenLens[Functionality](_.dependencies)
  val repositoriesLens = GenLens[Functionality](_.repositories)
}
