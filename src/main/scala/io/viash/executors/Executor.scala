package io.viash.executors

import io.viash.schemas._

trait Executor {
  @description("Id of the executor.")
  @example("id: foo", "yaml")
  val id: String
}
