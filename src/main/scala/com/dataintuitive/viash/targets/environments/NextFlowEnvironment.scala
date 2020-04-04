package com.dataintuitive.viash.targets.environments

case class NextFlowEnvironment(
  executor: Option[String] = None,
  publishSubdir: Option[Boolean]
) {

  def getInstallCommands() = {
    List()
  }
}
