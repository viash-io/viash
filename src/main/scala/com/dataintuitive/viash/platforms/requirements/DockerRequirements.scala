package com.dataintuitive.viash.platforms.requirements

case class DockerRequirements(
  resources: List[String] = Nil,
  run: List[String] = Nil,
  build_args: List[String] = Nil
) extends Requirements {
  val `type` = "docker"

  def installCommands: List[String] = Nil

  override def dockerCommands: Option[String] = {
    val args =
      if (build_args.nonEmpty) {
        build_args.map(s => "ARG " + s.takeWhile(_ != '='))
      } else {
        Nil
      }

    val copyResources =
      if (resources.nonEmpty) {
        resources.map(c => s"""COPY $c""")
      } else {
        Nil
      }

    val runCommands =
      if (run.nonEmpty) {
        run.map(r => s"""RUN $r""")
      } else {
        Nil
      }

    val li = args ::: copyResources ::: runCommands

    if (li.isEmpty) None else Some(li.mkString("\n"))
  }


}
