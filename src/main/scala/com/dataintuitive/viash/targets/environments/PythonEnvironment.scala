package com.dataintuitive.viash.targets.environments

case class PythonEnvironment(
  packages: List[String] = Nil,
  github: List[String] = Nil
) {
  def getInstallCommands() = {
    val installPip =
      """pip install --upgrade pip"""

    val installCranPackages =
      packages match {
        case Nil => Nil
        case packs =>
          List(packs.mkString(
            "pip install --user --no-cache-dir ",
            " ",
            ""
          ))
      }

    val installGithubPackages =
      github match {
        case Nil => Nil
        case packs =>
          List(packs.mkString(
            "pip install --user --no-cache-dir git+https://github.com/",
            " git+https://github.com/",
            ""
          ))
      }

    installPip :: installCranPackages ::: installGithubPackages
  }
}
