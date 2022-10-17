package io.viash.lenses

import io.viash.config.Config
import io.viash.functionality.Functionality
import monocle.Lens
import monocle.macros.GenLens

import FunctionalityLenses._


object ConfigLenses {
  val functionalityLens = GenLens[Config](_.functionality)

  val composedDependenciesLens = functionalityLens.^|->(dependenciesLens)
  val composedRepositoriesLens = functionalityLens.^|->(repositoriesLens)
}
