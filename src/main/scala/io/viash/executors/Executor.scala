package io.viash.executors

import io.viash.schemas._
import io.viash.functionality.Functionality
import io.viash.config.Config
import io.viash.platforms.Platform

trait Executor {
  @description("Specifies the type of the platform.")
  val `type`: String

  @description("Id of the executor.")
  @example("id: foo", "yaml")
  val id: String

  def generateExecutor(config: Config, testing: Boolean): ExecutorResources
}

object Executor{
  // Helper method to fascilitate conversion of legacy code to the new methods
  def get(platform: Platform) = {
    platform match {
      case p: Executor => p
      case _ => throw new RuntimeException("Expected all legacy platforms to be executors")
    }
  }
}

