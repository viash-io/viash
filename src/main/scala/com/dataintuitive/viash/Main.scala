package com.dataintuitive.viash

import functionality._
import platforms._
import resources._
import helpers.{Exec, IOHelper}
import config.Config
import meta.Meta

import java.nio.file.{Paths, Files}
import scala.io.Source
import org.rogach.scallop.{Subcommand, ScallopOption}

import sys.process._

object Main {
  private val pkg = getClass.getPackage
  val name = pkg.getImplementationTitle
  val version = pkg.getImplementationVersion

  def main(args: Array[String]) {
    val (viashArgs, runArgs) = args.span(_ != "--")

    val conf = new CLIConf(viashArgs)

    conf.subcommand match {
      case Some(conf.run) => {
        // create new functionality with argparsed executable
        val (fun, tar) = viashLogic(conf.run)

        // make temporary directory
        val dir = IOHelper.makeTemp("viash_" + fun.name)

        try {
          // write executable and resources to temporary directory
          writeResources(fun.resources.getOrElse(Nil), dir)

          // determine command
          val cmd =
            Array(Paths.get(dir.toString(), fun.name).toString()) ++
            runArgs.dropWhile(_ == "--")

          // execute command, print everything to console
          val code = Process(cmd).!(ProcessLogger(println, println))
          System.exit(code)
        } finally {
          // always remove tempdir afterwards
          if (!conf.run.keep()) {
            IOHelper.deleteRecursively(dir)
          } else {
            println(s"Files and logs are stored at '$dir'")
          }
        }
      }
      case Some(conf.run2) => {
        // create new functionality with argparsed executable
        val config = viashLogic2(conf.run2)
        val fun = config.functionality

        // make temporary directory
        val dir = IOHelper.makeTemp("viash_" + fun.name)

        try {
          // write executable and resources to temporary directory
          writeResources(fun.resources.getOrElse(Nil), dir)

          // determine command
          val cmd =
            Array(Paths.get(dir.toString(), fun.name).toString()) ++
            runArgs.dropWhile(_ == "--")

          // execute command, print everything to console
          val code = Process(cmd).!(ProcessLogger(println, println))
          System.exit(code)
        } finally {
          // always remove tempdir afterwards
          if (!conf.run.keep()) {
            IOHelper.deleteRecursively(dir)
          } else {
            println(s"Files and logs are stored at '$dir'")
          }
        }
      }
      case Some(conf.export) => {
        // create new functionality with argparsed executable
        val (fun, tar) = viashLogic(conf.export)

        // write files to given output directory
        val dir = new java.io.File(conf.export.output())
        dir.mkdirs()

        val execPath = Paths.get(dir.toString(), fun.mainScript.get.filename).toString()
        val functionalityPath = conf.export.functionality()
        val platformPath = conf.export.platform.getOrElse("")
        val outputPath = conf.export.output()
        val executablePath = execPath

        val meta = Meta(
          "v" + version,
          fun,
          tar,
          functionalityPath,
          platformPath,
          outputPath,
          executablePath
        )

        writeResources(meta.resource :: fun.resources.getOrElse(Nil), dir)

        if (conf.export.meta()) {
          println(meta.info)
        }
      }
      case Some(conf.test) => {
        val fun = readFunctionality(conf.test.functionality)
        val platform = readPlatform(conf.test.platform)
        val verbose = conf.test.verbose()

        // create temporary directory
        val dir = IOHelper.makeTemp("viash_test_" + fun.name)

        val results = ViashTester.runTests(fun, platform, dir, verbose = verbose)

        val code = ViashTester.reportTests(results, dir, verbose = verbose)

        if (!conf.test.keep() && !results.exists(_.exitValue > 0)) {
          println("Cleaning up temporary files")
          IOHelper.deleteRecursively(dir)
        } else {
          println(s"Test files and logs are stored at '$dir'")
        }

        System.exit(code)
      }
      case _ => println("No subcommand was specified. See `viash --help` for more information.")
    }
  }

  def readFunctionality(opt: ScallopOption[String]) = {
    Functionality.parse(IOHelper.uri(opt()))
  }
  def readComponent(opt: ScallopOption[String]) = {
    val uri = IOHelper.uri(opt())

    val str = IOHelper.read(uri)
    val uris = uri.toString()
    val extension = uris.substring(uris.lastIndexOf(".")+1).toLowerCase()

    // detect whether a component was passed or a yaml
    // using the extension
    val (yaml, code) =
      if (extension == "yml" || extension == "yaml") {
        (str, None)
      } else {
        val commentStr = extension match {
          case "sh" | "py" | "r" => "#'"
          case _ => throw new RuntimeException("Unrecognised extension: " + extension)
        }
        val headerComm = commentStr + " "
        val headerRegex = "^" + commentStr + "  ".r
        assert(
          str.contains(s"$commentStr functionality:"),
          message = s"""Component should contain a functionality header: "$commentStr functionality: <...>""""
        )

        val (header, body) = str.split("\n").partition(_.startsWith(headerComm))
        val yaml = header.map(s => s.drop(3)).mkString("\n")
        val code = commentStr + " VIASH START\n" + commentStr + "VIASH END\n" + body.mkString("\n")

        (yaml, Some(code))
      }

    // turn optional code into a Script
    val componentScript = code.map{cod =>
      val scr = extension match {
        case "r" => RScript(Some("viash_main.R"), text = Some(cod))
        case "py" => PythonScript(Some("viash_main.py"), text = Some(cod))
        case "sh" => BashScript(Some("viash_main.sh"), text = Some(cod))
        case _ => throw new RuntimeException("Unrecognised extension: " + extension)
      }
      scr.asInstanceOf[Script]
    }

    // read config
    val config = Config.parse(yaml, uri)

    config.copy(
      functionality = config.functionality.copy(
         resources = Some(componentScript.toList ::: config.functionality.resources.getOrElse(Nil))
      )
    )
  }

  def readPlatform(opt: ScallopOption[String]) = {
    opt.map{ path =>
      Platform.parse(IOHelper.uri(path))
    }.getOrElse(NativePlatform())
  }

  def viashLogic(subcommand: WithFunctionality with WithPlatform) = {
    // get the functionality yaml
    // let the functionality object know the path in which it resided,
    // so it can find back its resources
    val functionality = readFunctionality(subcommand.functionality)

    // get the platform
    // if no platform is provided, assume the platform
    // should be native and all dependencies are taken care of
    val platform = readPlatform(subcommand.platform)

    // modify the functionality using the platform
    val fun2 = platform.modifyFunctionality(functionality)

    (fun2, platform)
  }


  def viashLogic2(subcommand: ViashCommand) = {
    // read the component if passed, else read the functionality
    assert(
      subcommand.component.isEmpty != subcommand.functionality.isEmpty,
      message = "Either functionality or component need to be specified!"
    )
    val config =
      if (subcommand.component.isDefined) {
        readComponent(subcommand.component)
      } else {
        Config(
          functionality = readFunctionality(subcommand.functionality)
        )
      }

    // get the platform
    // * if a platform yaml is passed, use that
    // * else if a platform id is passed, look up the platform in the platforms list
    // * else if a platform is already defined in the config, use that
    // * else if platforms is a non-empty list, use the first platform
    // * else use the native platform
    val platform =
      if (subcommand.platform.isDefined) {
        Platform.parse(IOHelper.uri(subcommand.platform()))
      } else if (subcommand.platformID.isDefined) {
        val pid = subcommand.platformID()
        val platformNames = config.platforms.map(_.id)
        assert(
          platformNames.contains(pid),
          s"platform $pid is not found amongst the defined platforms: ${platformNames.mkString(", ")}"
        )
        config.platforms(platformNames.indexOf(pid))
      } else if (config.platform.isDefined) {
        config.platform.get
      } else if (!config.platforms.isEmpty) {
        config.platforms(0)
      } else {
        NativePlatform()
      }

    // modify the functionality using the platform
    config.copy(
      functionality = platform.modifyFunctionality(config.functionality),
      platform = Some(platform)
    )
  }

  def writeResources(
    resources: Seq[Resource],
    outputDir: java.io.File,
    overwrite: Boolean = true
  ) {
    // copy all files
    resources.foreach{ resource =>
      val dest = Paths.get(outputDir.getAbsolutePath, resource.filename)
      resource.write(dest, overwrite)
    }
  }

}
