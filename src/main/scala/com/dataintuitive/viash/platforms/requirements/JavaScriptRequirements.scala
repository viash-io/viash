package com.dataintuitive.viash.platforms.requirements

case class JavaScriptRequirements(
  packages: List[String] = Nil,
  npm: List[String] = Nil,
  git: List[String] = Nil,
  github: List[String] = Nil,
  url: List[String] = Nil
) extends Requirements {

  val `type` = "python"

  private def generateCommands(prefix: String, values: List[String]) = {
    values match {
      case Nil => Nil
      case packs =>
        List(packs.mkString(
          "npm install -g \"" + prefix,
          "\" \"" + prefix,
          "\""))
    }
  }

  def installCommands: List[String] = {
    val installNpmPackages = generateCommands("", npm ::: packages)
    val installGitPackages = generateCommands("git+", git)
    val installGithubPackages = generateCommands("", github)
    val installUrlPackages = generateCommands("", url)

    installNpmPackages ::: installGitPackages ::: installGithubPackages ::: installUrlPackages
  }
}
