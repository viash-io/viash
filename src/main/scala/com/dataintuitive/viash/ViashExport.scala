package com.dataintuitive.viash

import config._
import functionality.resources.PlainFile
import io.circe.yaml.Printer
import helpers.IOHelper

object ViashExport {
  def export(config: Config, output: String, printMeta: Boolean = false) {
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
    val strippedConfig = config.copy(
      functionality = fun.copy(
        resources = Some(fun.resources.get.tail) // drop main script
      ),
      platforms = Nil // drop other platforms
    )

    val configYaml = PlainFile(
      name = Some("viash.yaml"),
      text = Some(printer.pretty(encodeConfig(strippedConfig)))
    )

    IOHelper.writeResources(configYaml :: fun.resources.getOrElse(Nil), dir)

    if (printMeta) {
      println(config.info.get.consoleString)
    }
  }
}