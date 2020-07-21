package com.dataintuitive.viash.platforms.requirements

case class AptRequirements(
  packages: List[String] = Nil
) extends Requirements {
  val `type` = "apt"

  def installCommands = {
    val aptUpdate =
      """apt-get update"""

    val installPackages =
      packages match {
        case Nil => Nil
        case packs =>
          List(packs.mkString(
            "apt-get install -y ",
            " ",
            ""
          ))
      }

    val clean = "rm -rf /var/lib/apt/lists/*"

    aptUpdate :: installPackages ::: List(clean)
  }
}
