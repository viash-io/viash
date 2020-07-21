package com.dataintuitive.viash.platforms.requirements

case class DockerRequirements(
  resources: List[String] = Nil,
  run: List[String] = Nil
) extends Requirements {
  val `type` = "docker"

  def installCommands = Nil

  override def dockerCommands = {
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

    val li = copyResources ::: runCommands

    if (li.isEmpty) None else Some(li.mkString("\n"))
  }


}
