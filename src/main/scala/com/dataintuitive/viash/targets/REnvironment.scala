package com.dataintuitive.viash.targets

case class REnvironment(
  packages: Option[List[String]] = None,
  github: Option[List[String]] = None
) {
  def getInstallCommands() = {
    val installRemotes = 
      """Rscript -e 'if (!requireNamespace("remotes")) install.packages("remotes")'"""
      
    val installCranPackages = 
      packages.getOrElse(Nil) match {
        case Nil => Nil
        case packs => 
          List(packs.mkString(
            "Rscript -e 'remotes::install_cran(c(\"", 
            "\", \"", 
            "\"))'"
          ))
      }
    
    val installGithubPackages = 
      github.getOrElse(Nil) match {
        case Nil => Nil
        case packs => 
          List(packs.mkString(
            "Rscript -e 'remotes::install_github(c(\"", 
            "\", \"", 
            "\"))'"
          ))
      }
      
    installRemotes :: installCranPackages ::: installGithubPackages
  }
}
