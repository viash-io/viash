package com.dataintuitive.viash.targets.environments

case class NextFlowEnvironment(
  executor: Option[String] = None
) {

  def getInstallCommands() = {
    List()
  }
}
