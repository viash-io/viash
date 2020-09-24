package com.dataintuitive.viash

import config.Config

object Main {
  private val pkg = getClass.getPackage
  val name: String = if (pkg.getImplementationTitle != null) pkg.getImplementationTitle else "viash"
  val version: String = if (pkg.getImplementationVersion != null) pkg.getImplementationVersion else "test"

  def main(args: Array[String]) {
    val (viashArgs, runArgs) = args.span(_ != "--")

    val conf = new CLIConf(viashArgs)

    conf.subcommands match {
      case List(conf.run) =>
        val config = readConfigFromArgs(conf.run)
        ViashRun(config, args = runArgs.dropWhile(_ == "--"), keepFiles = conf.run.keep())
      case List(conf.build) =>
        val config = readConfigFromArgs(conf.build)
        ViashBuild(config, output = conf.build.output(), printMeta = conf.build.meta(), setup = conf.build.setup())
      case List(conf.test) =>
        val config = readConfigFromArgs(conf.test, modifyFun = false)
        ViashTest(config, verbose = conf.test.verbose(), keepFiles = conf.test.keep())
      case List(conf.namespace, conf.namespace.build) =>
        ViashNamespace.build(
          source = conf.namespace.build.src(),
          target = conf.namespace.build.target(),
          platform = conf.namespace.build.platform.toOption,
          platformID = conf.namespace.build.platformid.toOption,
          namespace = conf.namespace.build.namespace.toOption,
          setup = conf.namespace.build.setup(),
          parallel = conf.namespace.build.parallel()
        )
      case _ =>
        println("No subcommand was specified. See `viash --help` for more information.")
    }
  }

  def readConfigFromArgs(
    subcommand: ViashCommand,
    modifyFun: Boolean = true
  ): Config = {
    Config.readSplitOrJoined(
      joined = subcommand.config.toOption,
      functionality = subcommand.functionality.toOption,
      platform = subcommand.platform.toOption,
      platformID = subcommand.platformid.toOption,
      modifyFun = modifyFun
    )
  }
}
