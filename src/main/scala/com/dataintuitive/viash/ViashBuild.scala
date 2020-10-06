package com.dataintuitive.viash

import config._
import functionality.resources.PlainFile
import io.circe.yaml.Printer
import helpers.IO
import java.nio.file.Paths

import scala.sys.process.{Process, ProcessLogger}

object ViashBuild {
  def apply(
    config: Config,
    output: String,
    printMeta: Boolean = false,
    namespace: Option[String] = None,
    setup: Boolean = false
  ) {
    val fun = config.functionality

    // create dir
    val dir = new java.io.File(output)
    dir.mkdirs()

    // create a yaml printer for writing the viash.yaml file
    // Options: https://github.com/circe/circe-yaml/blob/master/src/main/scala/io/circe/yaml/Printer.scala
    val printer = Printer(
      preserveOrder = true,
      dropNullKeys = true,
      mappingStyle = Printer.FlowStyle.Block,
      splitLines = true
    )

    // get the path of where the executable will be written to
    val exec_path = fun.mainScript.map(scr => Paths.get(output, scr.name.get).toString)

    // change the config object before writing to yaml:
    // * add more info variables
    // * remove other platforms other than the one finally used
    // * override namespace in functionality
    val strippedConfig = config.copy(
      info = config.info.map(_.copy(
        output_path = Some(output),
        executable_path = exec_path
      )),
      platforms = Nil // drop other platforms
    ).copy(
      functionality = config.functionality.copy(namespace = namespace)
    )

    // add yaml to resources
    val configYamlStr = printer.pretty(encodeConfig(strippedConfig))
    // TODO: manually change text to a nice format?
    val configYaml = PlainFile(
      name = Some("viash.yaml"),
      text = Some(configYamlStr)
    )

    // write resources to output directory
    IO.writeResources(configYaml :: fun.resources.getOrElse(Nil), dir)

    // if '--setup' was passed, run './executable ---setup'
    if (setup && exec_path.isDefined) {
      val cmd = Array(exec_path.get, "---setup")
      val _ = Process(cmd).!(ProcessLogger(println, println))
    }

    // if '-m' was passed, print some yaml about the created output fiels
    if (printMeta) {
      println(config.info.get.consoleString)
    }
  }
}
