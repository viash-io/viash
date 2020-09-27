package com.dataintuitive.viash

import org.rogach.scallop.{ScallopConf, ScallopOption, Subcommand}

trait ViashCommand {
  _: ScallopConf =>
  val functionality = opt[String](
    descr = "[deprecated] Path to the functionality file.",
    default = None,
    required = false
  )
  val platform = opt[String](
    short = 'p',
    default = None,
    descr = "Path to a custom platform file.",
    required = false
  )
  val platformid = opt[String](
    short = 'P',
    default = None,
    descr = "If multiple platforms are specified in the config, use the platform with this name.",
    required = false
  )
  val config = trailArg[String](
    descr = "A viash config file (example: conf.vsh.yaml). This argument can also be a script with the config as a header.",
    default = None,
    required = false
  )
}

trait WithTemporary {
  _: ScallopConf =>
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

  banner(
    s"""
       |viash is a spec and a tool for defining execution contexts and converting execution instructions to concrete instantiations.
       |
       |Usage:
       |  viash run config.vsh.yaml -- [arguments for script]
       |  viash build config.vsh.yaml
       |  viash test config.vsh.yaml
       |  viash ns build
       |
       |Check the help of a subcommand for more information, or the API available at:
       |  https://www.data-intuitive.com/viash_docs
       |
       |Arguments:""".stripMargin)

  val run = new Subcommand("run") with ViashCommand with WithTemporary {
    banner(
      s"""viash run
         |Executes a viash component from the provided viash config file. viash generates a temporary executable and immediately executes it with the given parameters.
         |
         |Usage:
         |  viash run [-P docker/-p platform.yaml] [-k] config.vsh.yaml -- [arguments for script]
         |
         |Arguments:""".stripMargin)

    footer(
      s"""  -- param1 param2 ...    Extra parameters to be passed to the component itself.
         |                          -- is used to separate viash arguments from the arguments
         |                          of the component.
         |
         |The temporary directory can be altered by setting the VIASH_TEMP directory. Example:
         |  export VIASH_TEMP=/home/myuser/.viash_temp
         |  viash run -k config.vsh.yaml""".stripMargin)
  }

  val build = new Subcommand("build") with ViashCommand {
    banner(
      s"""viash build
         |Build an executable from the provided viash config file.
         |
         |Usage:
         |  viash build -o output [-P docker/-p platform.yaml] [-m] [-s] config.vsh.yaml
         |
         |Arguments:""".stripMargin)

    val meta = opt[Boolean](
      name = "meta",
      short = 'm',
      default = Some(false),
      descr = "Print out some meta information at the end."
    )
    val output = opt[String](
      descr = "Path to directory in which the executable and any resources is built to. Default: \"output/\".",
      default = Some("output/"),
      required = true
    )
    val setup = opt[Boolean](
      name = "setup",
      default = Some(false),
      descr = "Whether or not to set up the platform environment after building the executable."
    )
  }

  val test = new Subcommand("test") with ViashCommand with WithTemporary {
    banner(
      s"""viash test
         |Test the component using the tests defined in the viash config file.
         |
         |Usage:
         |  viash test [-P docker/-p platform.yaml] [-v] [-k] config.vsh.yaml
         |
         |Arguments:""".stripMargin)

    footer(
      s"""
         |The temporary directory can be altered by setting the VIASH_TEMP directory. Example:
         |  export VIASH_TEMP=/home/myuser/.viash_temp
         |  viash run -k meta.vsh.yaml""".stripMargin)
  }

  val namespace = new Subcommand("ns") {

    val build = new Subcommand("build") {
      banner(
        s"""viash ns build
           |Build a namespace from many viash config files.
           |
           |Usage:
           |  viash ns build [-s src] [-t target] [-P docker/-p platform.yaml] [--setup] [--parallel]
           |
           |Arguments:""".stripMargin)

      val namespace = opt[String](
        name = "namespace",
        short = 'n',
        descr = "The name of the namespace.",
        default = None
      )
      val src = opt[String](
        name = "src",
        short = 's',
        descr = " A source directory containing viash config files, possibly structured in a hierarchical folder structure. Default: src/.",
        default = Some("src")
      )
      val target = opt[String](
        name = "target",
        short = 't',
        descr = "A target directory to build the executables into. Default: target/.",
        default = Some("target")
      )
      val platform = opt[String](
        short = 'p',
        descr = "Path to a custom platform file.",
        default = None,
        required = false
      )
      val platformid = opt[String](
        short = 'P',
        descr = "Only build a particular platform type.",
        default = None,
        required = false
      )
      val setup = opt[Boolean](
        name = "setup",
        default = Some(false),
        descr = "Whether or not to set up the platform environment after building the executable."
      )
      val parallel = opt[Boolean](
        name = "parallel",
        short = 'l',
        default = Some(false),
        descr = "Whether or not to run the process in parallel."
      )
    }

    val test = new Subcommand("test") {
      banner(
        s"""viash ns test
           |Test a namespace containing many viash config files.
           |
           |Usage:
           |  viash ns test [-s src] [-P docker/-p platform.yaml] [--parallel]
           |
           |Arguments:""".stripMargin)

      val namespace = opt[String](
        name = "namespace",
        short = 'n',
        descr = "The name of the namespace.",
        default = None
      )
      val src = opt[String](
        name = "src",
        short = 's',
        descr = " A source directory containing viash config files, possibly structured in a hierarchical folder structure. Default: src/.",
        default = Some("src")
      )
      val platform = opt[String](
        short = 'p',
        descr = "Path to a custom platform file.",
        default = None,
        required = false
      )
      val platformid = opt[String](
        short = 'P',
        descr = "Only build a particular platform type.",
        default = None,
        required = false
      )
      val parallel = opt[Boolean](
        name = "parallel",
        short = 'l',
        default = Some(false),
        descr = "Whether or not to run the process in parallel."
      )
    }

    addSubcommand(build)
    addSubcommand(test)
    requireSubcommand()

    shortSubcommandsHelp(true)
  }

  addSubcommand(run)
  addSubcommand(build)
  addSubcommand(test)
  addSubcommand(namespace)

  shortSubcommandsHelp(true)

  verify()
}