package com.dataintuitive.viash

import org.rogach.scallop.{Scallop, ScallopConf, Subcommand, ScallopHelpFormatter}

trait ViashCommand {_ : ScallopConf =>
  val functionality = opt[String](
    descr = "Path to the functionality file.",
    default = None,
    required = false
  )
  val platform = opt[String](
    short = 'p',
    default = None,
    descr = "Path to the platform file. If not provided, the native platform is used.",
    required = false
  )
  val platformID = opt[String](
    short = 'P',
    default = None,
    descr = "If multiple platforms are specified in the component, this argument allows you to choose which one.",
    required = false
  )
  val component = trailArg[String](
    descr = "A viash component",
    default = None,
    required = false
  )
}
trait WithTemporary { _: ScallopConf =>
  val keep = opt[Boolean](
    name = "keep",
    short = 'k',
    default = Some(false),
    descr = "Do not remove temporary files. The temporary directory can be overwritten by setting defining a VIASH_TEMP directory."
  )
}

class CLIConf(arguments: Seq[String]) extends ScallopConf(arguments) {
  version(s"${Main.name} ${Main.version} (c) 2020 Data Intuitive")

  appendDefaultToDescription = true

  banner(s"""
    |viash is a spec and a tool for defining execution contexts and converting execution instructions to concrete instantiations.
    |
    |Usage:
    |  viash run script.sh [-P docker] [-k] [-- --params --to component]
    |  viash export script.sh [-P docker] -o output [-m]
    |  viash test script.sh [-P docker] [-v] [-k]
    |
    |Check the help of a subcommand for more information, or the API available at:
    |  https://github.com/data-intuitive/viash_docs
    |
    |Arguments:""".stripMargin)

  val run = new Subcommand("run") with ViashCommand with WithTemporary {
    banner(s"""viash run
      |Executes a viash component. From the provided functionality.yaml, viash generates a temporary executable and immediately executes it with the given parameters.
      |
      |Usage:
      |  viash run script.sh [-p platform.yaml] [-P docker] [-k] [-- --params --to component]
      |
      |Arguments:""".stripMargin)

    footer(s"""
      |The temporary directory can be altered by setting the VIASH_TEMP directory. Example:
      |  export VIASH_TEMP=/home/myuser/.viash_temp
      |  viash run -f fun.yaml -k""".stripMargin)
  }

  val export = new Subcommand("export") with ViashCommand {
    banner(s"""viash export
      |Generate an executable from the functionality and platform meta information.
      |
      |Usage:
      |  viash export script.sh [-p platform.yaml] [-P docker] -o output [-m]
      |
      |Arguments:""".stripMargin)

    val meta = opt[Boolean](
        name = "meta",
        short = 'm',
        default = Some(false),
        descr = "Print out some meta information at the end."
      )
    val output = opt[String](
      descr = "Path to directory in which the executable and any resources is exported to. Default: \"output/\".",
      default = Some("output/"),
      required = true
    )
  }
  val test = new Subcommand("test") with ViashCommand with WithTemporary {
    banner(s"""viash test
      |Run the tests as defined in the functionality.yaml. Check the documentation for more information on how to write tests.
      |
      |Usage:
      |  viash test script.sh [-p platform.yaml] [-P docker] [-v] [-k]
      |
      |Arguments:""".stripMargin)

    val verbose = opt[Boolean](
      name = "verbose",
      short = 'v',
      default = Some(false),
      descr = "Print out all output from the tests. Otherwise, only a summary is shown."
    )

    footer(s"""
      |The temporary directory can be altered by setting the VIASH_TEMP directory. Example:
      |  export VIASH_TEMP=/home/myuser/.viash_temp
      |  viash run -f fun.yaml -k""".stripMargin)
  }
  
  val namespace = new Subcommand("namespace") {
    
    val build = new Subcommand("build") {
      val namespace = opt[String](
        name = "namespace",
        short = 'n',
        required = true,
        descr = "The name of the namespace."
      )
      val src = opt[String](
        name = "src",
        short = 's',
        descr = "An alternative source directory if not under src/<namespace>."
      )
      val target = opt[String](
        name = "target",
        short = 't',
        descr = "An alternative destination directory if not target/."
      )
    }
    
    addSubcommand(build)
    requireSubcommand()
  }

  addSubcommand(run)
  addSubcommand(export)
  addSubcommand(test)
  addSubcommand(namespace)

  shortSubcommandsHelp(true)

  verify()
}