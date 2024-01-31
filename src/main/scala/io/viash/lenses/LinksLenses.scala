package io.viash.lenses

import io.viash.functionality.Links

import monocle.Lens
import monocle.macros.GenLens

object LinksLenses {
  val repositoryLens = GenLens[Links](_.repository)
  val dockerRegistryLens = GenLens[Links](_.docker_registry)
  val homepageLens = GenLens[Links](_.homepage)
  val documentationLens = GenLens[Links](_.documentation)
  val issueTrackerLens = GenLens[Links](_.issue_tracker)
}