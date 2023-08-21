package io.viash.containers

import io.viash.schemas._

trait Container {
  @description("Id of the container.")
  @example("id: foo", "yaml")
  val id: String
}
