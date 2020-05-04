package com.dataintuitive.viash.targets.environments

case class AptEnvironment(
  packages: Option[List[String]] = None
) {
  def getInstallCommands() = {
    val aptUpdate =
      """apt-get update"""

    val installPackages =
      packages.getOrElse(Nil) match {
        case Nil => Nil
        case packs =>
          List(packs.mkString(
            "apt-get install -y ",
            " ",
            ""
          ))
      }

    aptUpdate :: installPackages
  }
}
