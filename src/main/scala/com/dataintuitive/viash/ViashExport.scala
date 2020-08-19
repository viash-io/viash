package com.dataintuitive.viash

import config._
import functionality.resources.PlainFile
import io.circe.yaml.Printer

object ViashExport {
  def export(config: Config, output: String, printMeta: Boolean) {
    val fun = config.functionality
    val plat = config.platform.get

    // create dir
    val dir = new java.io.File(output)
    dir.mkdirs()

    // create Config Resource
    // Options: https://github.com/circe/circe-yaml/blob/master/src/main/scala/io/circe/yaml/Printer.scala
    val printer = Printer(
      dropNullKeys = true,
      mappingStyle = Printer.FlowStyle.Block,
      splitLines = true
    )
    val configJson = encodeConfig(config)

//    val configResource = PlainFile(
//      name = Some("viash.yaml"),
//      text = Some(printer.pretty(configJson))
//    )

//    Main.writeResources(configResource :: fun.resources.getOrElse(Nil), dir)

    Main.writeResources(fun.resources.getOrElse(Nil), dir)

    if (printMeta) {
      println(config.info.get.consoleString)
    }
  }
}