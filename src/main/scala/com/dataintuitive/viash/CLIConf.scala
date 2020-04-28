package com.dataintuitive.viash

import org.rogach.scallop.{ScallopConf, Subcommand}

trait CommonParams { _: ScallopConf =>
  val functionality = opt[String](
    descr = "Path to the functionality specifications YAML file",
    required = true
  )
  val platform = opt[String](
    default = None,
    descr = "Path to the platform specifications YAML file",
    required = false
  )
}

class CLIConf(arguments: Seq[String]) extends ScallopConf(arguments) {
  val run = new Subcommand("run") with CommonParams
  val export = new Subcommand("export") with CommonParams {
    val output = opt[String](
      descr = "Path to directory.",
      default = Some("output/"),
      required = true
    )
  }

  addSubcommand(run)
  addSubcommand(export)

  verify()
}