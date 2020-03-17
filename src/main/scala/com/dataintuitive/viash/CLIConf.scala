package com.dataintuitive.viash

import org.rogach.scallop.{ScallopConf, Subcommand}

class CLIConf(arguments: Seq[String]) extends ScallopConf(arguments) {
  val functionality = opt[String](
    default = Some("functionality.yaml"), 
    descr = "Path to the functionality specifications (default functionality.yaml)",
    required = true
  )
  val platform = opt[String](
    default = Some("platform.yaml"), 
    descr = "Path to the platform specifications (default platform.yaml)",
    required = true
  )
  
  val run = new Subcommand("run") {
    val test = opt[String](
      descr = "Test argument"
    )
  }
  val export = new Subcommand("export") {
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