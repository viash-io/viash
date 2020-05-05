package com.dataintuitive.viash

import org.rogach.scallop.{ScallopConf, Subcommand}

trait WithFunctionality { _: ScallopConf =>
  val functionality = opt[String](
    descr = "Path to the functionality specifications YAML file",
    required = true
  )
}
trait WithPlatform { _: ScallopConf =>
  val platform = opt[String](
    default = None,
    descr = "Path to the platform specifications YAML file",
    required = false
  )
}

class CLIConf(arguments: Seq[String]) extends ScallopConf(arguments) {
  val run = new Subcommand("run") with WithFunctionality with WithPlatform
  val export = new Subcommand("export") with WithFunctionality with WithPlatform {
    val output = opt[String](
      descr = "Path to directory.",
      default = Some("output/"),
      required = true
    )
  }
  val pimp = new Subcommand("pimp") with WithFunctionality {
    val output = opt[String](
      descr = "Path to write the pimped script to.",
      required = false
    )
  }

  addSubcommand(run)
  addSubcommand(export)
  addSubcommand(pimp)

  verify()
}