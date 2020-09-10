package com.dataintuitive.viash.platforms.requirements

case class PythonRequirements(
  packages: List[String] = Nil,
  github:   List[String] = Nil
) extends Requirements {

  val `type` = "python"

  def installCommands: List[String] = {
    val installPip =
      """pip install --user --upgrade pip"""

    val installPipPackages =
      packages match {
        case Nil => Nil
        case packs =>
          List(packs.mkString(
            "pip install --user --no-cache-dir \"",
            "\" \"",
            "\""))
      }

    val installGithubPackages =
      github match {
        case Nil => Nil
        case packs =>
          List(packs.mkString(
            "pip install --user --no-cache-dir \"git+https://github.com/",
            "\" \"git+https://github.com/",
            "\""))
      }

    installPip :: installPipPackages ::: installGithubPackages
  }
}
