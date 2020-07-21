package com.dataintuitive.viash.targets.environments

case class DockerEnvironment(
  resources: List[String] = Nil,
  run: List[String] = Nil
) {
  def getInstallCommands() = {

    val copyResources =
      if (resources.length > 0) {
        resources.map(c => """COPY $c""")
      } else {
        Nil
      }

    val runCommands =
      if (run.length > 0) {
        run.map(r => """RUN $r""")
      } else {
        Nil
      }

    copyResources ::: runCommands
  }
}
