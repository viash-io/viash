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
    banner("""viash: write once, deploy anywhere
      |
      |Example: viash run -f functionality.yaml
      |
      |USAGE:""".stripMargin)

  val version = opt[Boolean](
    short = 'v',
    descr = "Print version"
  )

  val run = new Subcommand("run") with WithFunctionality with WithPlatform {
    val keep = opt[Boolean](
      name = "keep",
      short = 'k',
      default = Some(false),
      descr = "Do not remove temporary files"
    )
  }
  val export = new Subcommand("export") with WithFunctionality with WithPlatform {
    val meta = opt[Boolean](
        name = "meta",
        short = 'm',
        default = Some(false),
        descr = "Print out some meta information at the end"
      )
    val output = opt[String](
      descr = "Path to directory.",
      default = Some("output/"),
      required = true
    )
  }
  val test = new Subcommand("test") with WithFunctionality with WithPlatform {
    val verbose = opt[Boolean](
      name = "verbose",
      short = 'v',
      default = Some(false),
      descr = "Print out all output from the tests"
    )
    val keep = opt[Boolean](
      name = "keep",
      short = 'k',
      default = Some(false),
      descr = "Do not remove temporary files"
    )
  }

  addSubcommand(run)
  addSubcommand(export)
  addSubcommand(test)

  verify()
}