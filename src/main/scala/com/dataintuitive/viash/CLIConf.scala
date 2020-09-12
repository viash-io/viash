package com.dataintuitive.viash

import org.rogach.scallop.{ScallopConf, ScallopOption, Subcommand}

trait ViashCommand {
  _: ScallopConf =>
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
  val platformid = opt[String](
    short = 'P',
    default = None,
    descr = "If multiple platforms are specified in the component, this argument allows you to choose which one.",
    required = false
  )
  val joined = trailArg[String](
    descr = "A joined metadata file. Can also be a script written in R/Python/Bash but containing the joined metadata as a header.",
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
       |  viash run [arguments] script.vsh.yaml -- [arguments for script]
       |  viash build [arguments] script.vsh.sh
       |  viash test [arguments] script.vsh.R
       |  viash ns build [arguments]
       |
       |Check the help of a subcommand for more information, or the API available at:
       |  https://github.com/data-intuitive/viash_docs
       |
       |Arguments:""".stripMargin)

  val run = new Subcommand("run") with ViashCommand with WithTemporary {
    banner(
      s"""viash run
         |Executes a viash component. From the provided functionality.yaml, viash generates a temporary executable and immediately executes it with the given parameters.
         |
         |Usage:
         |  viash run [-P docker] [-k] script.vsh.sh [arguments for script]
         |
         |Arguments:""".stripMargin)

    footer(
      s"""
         |The temporary directory can be altered by setting the VIASH_TEMP directory. Example:
         |  export VIASH_TEMP=/home/myuser/.viash_temp
         |  viash run -f fun.yaml -k""".stripMargin)
  }

  val build = new Subcommand("build") with ViashCommand {
    banner(
      s"""viash build
         |Generate an executable from the functionality and platform meta information.
         |
         |Usage:
         |  viash build -o output [-P docker] [-m] script.vsh.sh
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
      descr = "Whether or not to run setup after build."
    )
  }

  val test = new Subcommand("test") with ViashCommand with WithTemporary {
    banner(
      s"""viash test
         |Run the tests as defined in the functionality.yaml. Check the documentation for more information on how to write tests.
         |
         |Usage:
         |  viash test [-P docker] [-v] [-k] script.vsh.sh
         |
         |Arguments:""".stripMargin)

    val verbose = opt[Boolean](
      name = "verbose",
      short = 'v',
      default = Some(false),
      descr = "Print out all output from the tests. Otherwise, only a summary is shown."
    )

    footer(
      s"""
         |The temporary directory can be altered by setting the VIASH_TEMP directory. Example:
         |  export VIASH_TEMP=/home/myuser/.viash_temp
         |  viash run -f fun.yaml -k""".stripMargin)
  }

  val namespace = new Subcommand("ns") {

    val build = new Subcommand("build") {
      banner(
        s"""viash ns build
           |Build a namespace containing many viash components.
           |
           |Usage:
           |  viash ns build [-s src] [-t target] [-P docker] [-p platform_docker.yaml]
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
        descr = "An alternative source directory if not under src/<namespace>. Default = source/.",
        default = Some("src")
      )
      val target = opt[String](
        name = "target",
        short = 't',
        descr = "An alternative destination directory if not target/. Default = target/.",
        default = Some("target")
      )
      val platform = opt[String](
        short = 'p',
        descr = "Path to the platform file. If not provided, the native platform is used.",
        default = None,
        required = false
      )
      val platformid = opt[String](
        short = 'P',
        descr = "If multiple platforms are specified in the component, this argument allows you to choose which one.",
        default = None,
        required = false
      )
      val setup = opt[Boolean](
        name = "setup",
        default = Some(false),
        descr = "Whether or not to run setup after build."
      )
      val parallel = opt[Boolean](
        name = "parallel",
        short = 'l',
        default = Some(false),
        descr = "Whether or not to run the process in parallel."
      )
    }

    addSubcommand(build)
    requireSubcommand()
  }

  addSubcommand(run)
  addSubcommand(build)
  addSubcommand(test)
  addSubcommand(namespace)

  shortSubcommandsHelp(true)

  verify()
}