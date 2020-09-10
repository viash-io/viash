package com.dataintuitive.viash.platforms.requirements

case class ApkRequirements(
  packages: List[String] = Nil
) extends Requirements {
  val `type` = "apk"

  val installCommands: List[String] = {
    packages match {
      case Nil => Nil
      case packs =>
        List(packs.mkString(
          "apk add --no-cache ",
          " ",
          ""
        ))
    }
  }
}
