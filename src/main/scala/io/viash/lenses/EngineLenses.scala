package io.viash.lenses

import io.viash.engines.DockerEngine

import monocle.Lens
import monocle.macros.GenLens

object EngineLenses {
  val packageLens = GenLens[DockerEngine](_.`package`)
  val organizationLens = GenLens[DockerEngine](_.organization)
}
