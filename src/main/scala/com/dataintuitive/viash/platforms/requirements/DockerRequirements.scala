package com.dataintuitive.viash.platforms.requirements

case class DockerRequirements(
  resources: List[String] = Nil,
  run: List[String] = Nil,
  build_args: List[String] = Nil
) extends Requirements {
  val `type` = "docker"

  def installCommands = Nil

  def dockerCommandsAtBegin = {
    val args =
      if (build_args.length > 0) {
        build_args.map(s => "ARG " + s.takeWhile(_ != '='))
      } else {
        Nil
      }

    val li = args
    if (li.isEmpty) None else Some(li.mkString("\n"))
  }

  override def dockerCommands = {
    val copyResources =
      if (resources.length > 0) {
        resources.map(c => s"""COPY $c""")
      } else {
        Nil
      }

    val runCommands =
      if (run.length > 0) {
        run.map(r => s"""RUN $r""")
      } else {
        Nil
      }

    val li = copyResources ::: runCommands

    if (li.isEmpty) None else Some(li.mkString("\n"))
  }


}
