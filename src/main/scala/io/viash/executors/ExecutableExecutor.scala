package io.viash.executors

import io.viash.schemas._
import io.viash.config.Config

final case class ExecutableExecutor(
  @description("Name of the executor. As with all executors, you can give an executor a different name. By specifying `id: foo`, you can target this executor (only) by specifying `...` in any of the Viash commands.")
  @example("id: foo", "yaml")
  @default("executable")
  id: String = "executable",
) extends Executor {
  val `type` = "executable"

  def generateExecutor(config: Config, testing: Boolean): ExecutorResources = {
    val containers = config.getContainers
    
    // todo: do something with containers

    // todo: generate mainscript
    val mainScript = None
    val additionalResources = Nil

    // return output
    ExecutorResources(
      mainScript = mainScript,
      additionalResources = additionalResources
    )
  }
}
