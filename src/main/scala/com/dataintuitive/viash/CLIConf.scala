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
  version(s"${Main.name} ${Main.version} (c) 2020 Data Intuitive")

  banner(s"""${Main.name}: from scripts to pipelines
    |Viash is a spec and a tool for defining execution contexts
    |and converting execution instructions to concrete instantiations.
    |
    |Usage:
    |  viash run -f functionality.yaml [-p platform.yaml] [-k]
    |  viash export -f functionality.yaml [-p platform.yaml] -o output [-m]
    |  viash test -f functionality.yaml [-p platform.yaml] [-v] [-k]
    |
    |API Documentation:
    |  https://github.com/data-intuitive/viash_docs/#viash-from-scripts-to-pipelines
    |
    |Options:""".stripMargin)

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