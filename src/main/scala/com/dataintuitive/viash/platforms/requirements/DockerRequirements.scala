package com.dataintuitive.viash.platforms.requirements

case class DockerRequirements(
  resources: List[String] = Nil,
  run: List[String] = Nil
) extends Requirements {
  val `type` = "docker"

  def installCommands = {

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
