package com.dataintuitive.viash.targets.environments

case class ApkEnvironment(
  packages: List[String] = Nil
) {
  def getInstallCommands() = {
    val installPackages =
      packages match {
        case Nil => Nil
        case packs =>
          List(packs.mkString(
            "apk add --no-cache ",
            " ",
            ""
          ))
      }

    installPackages
  }
}
