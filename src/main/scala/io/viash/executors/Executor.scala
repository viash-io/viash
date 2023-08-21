package io.viash.executors

import io.viash.schemas._
import io.viash.functionality.Functionality
import io.viash.config.Config

trait Executor {
  @description("Specifies the type of the platform.")
  val `type`: String

  @description("Id of the executor.")
  @example("id: foo", "yaml")
  val id: String

  def generateExecutor(config: Config, testing: Boolean): ExecutorResources
}

