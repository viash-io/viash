package com.dataintuitive.viash.targets

case class PythonEnvironment(
  packages: Option[List[String]] = None,
  github: Option[List[String]] = None
) {
  def getInstallCommands() = {
    val installPip = 
      """pip install --upgrade pip"""
      
    val installCranPackages = 
      packages.getOrElse(Nil) match {
        case Nil => Nil
        case packs => 
          List(packs.mkString(
            "pip install --user --no-cache-dir ", 
            " ", 
            ""
          ))
      }
    
    val installGithubPackages = 
      github.getOrElse(Nil) match {
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
