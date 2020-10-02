package com.dataintuitive.viash

import java.nio.file.{Files, Path, Paths}
import java.nio.file.attribute.BasicFileAttributes

import com.dataintuitive.viash.ViashTest.TestOutput
import config.Config
import config.Config.PlatformNotFoundException

import scala.collection.JavaConverters

object ViashNamespace {
  def find(sourceDir: Path, filter: (Path, BasicFileAttributes) => Boolean): List[Path] = {
    val it = Files.find(sourceDir, Integer.MAX_VALUE, (p, b) => filter(p, b)).iterator()
    JavaConverters.asScalaIterator(it).toList
  }

  def build(
    source: String,
    target: String,
    platform: Option[String] = None,
    platformID: Option[String] = None,
    namespace: Option[String] = None,
    setup: Boolean = false,
    parallel: Boolean = false
  ) {
    val configs = findConfigs(source, platform, platformID, namespace)

    val configs2 = if (parallel) configs.par else configs

    configs2.foreach {
      case Left(conf) =>
        val in = conf.info.get.parent_path.get
        val inTool = in.split("/").lastOption.getOrElse("WRONG")
        val platType = conf.platform.get.id
        val out =
          conf.functionality.namespace
            .map( ns => target + s"/$platType/$ns/$inTool").getOrElse(target + s"/$platType/$inTool")
        val namespaceOrNothing = conf.functionality.namespace.map( s => "(" + s + ")").getOrElse("")
        println(s"Exporting $in $namespaceOrNothing =$platType=> $out")
        ViashBuild(
          config = conf,
          output = out,
          namespace = conf.functionality.namespace,
          setup = setup
        )
      case Right(err) =>
        val in = err.config.info.get.parent_path.get
        println(s"Skipping $in --- platform ${err.platform} not found")
    }
  }

  def test(
    source: String,
    platform: Option[String] = None,
    platformID: Option[String] = None,
    namespace: Option[String] = None,
    parallel: Boolean = false,
    keepFiles: Option[Boolean] = None
  ) {
    val configs = findConfigs(source, platform, platformID, namespace)

    val configs2 = if (parallel) configs.par else configs

    val results =
      configs2.flatMap {
        case Left(conf) =>
          Some((conf, ViashTest(
            config = conf,
            keepFiles = keepFiles,
            quiet = true
          )))
        case Right(_) =>
          None
      }.toList

    for ((conf, (setupRes, testRes)) ← results) {
      val namespace = conf.functionality.namespace.getOrElse("")
      val funName = conf.functionality.name

      if (setupRes.exitValue > 0) {
        println(namespace + "-" + funName + " setup failed with exit code " + setupRes.exitValue + ":")
        println(setupRes.output)
        println()
      }

      for (test ← testRes if test.exitValue > 0) {
        println(namespace + "-" + funName + " test '" + test.name + "' failed with exit code " + test.exitValue + ":")
        println(test.output)
        println()
      }
    }

    printf("%20s %20s %20s %8s %8s %12s\n", "namespace", "functionality", "platform", "#success", "#tests", "result")
    for ((conf, (setupRes, testRes)) ← results) {
      val namespace = conf.functionality.namespace.getOrElse("")
      val funName = conf.functionality.name
      val platName = conf.platform.get.id

      val numTests = testRes.length // one is always the setup
      val numSucceeds = testRes.count(_.exitValue == 0) // one is always the setup
      val result =
        if (setupRes.exitValue > 0) {
          "SETUP ERROR"
        } else if (numTests == 0) {
          "No tests :("
        } else if (numSucceeds < numTests) {
          "FAIL!"
        } else {
          "Success!"
        }
      val col = if (result == "Success!") Console.GREEN else Console.RED
      printf("%s%20s %20s %20s %8d %8d %12s%s\n", col, namespace, funName, platName, numSucceeds, numTests, result, Console.RESET)
    }
  }

  /**
   * Given a Path to a viash config file (functionality.yaml / *.vsh.yaml),
   * extract an implicit namespace if appropriate.
   */
  private def getNamespace(namespace: Option[String], file: Path): Option[String] = {
    if (namespace.isDefined) {
      namespace
    } else {
      val parentDir = file.toString.split("/").dropRight(2).lastOption.getOrElse("src")
      if (parentDir != "src")
        Some(parentDir)
      else
        None
    }
  }

  def findConfigs(
    source: String,
    platform: Option[String] = None,
    platformID: Option[String] = None,
    namespace: Option[String]
  ): List[Either[Config, PlatformNotFoundException]] = {
    val sourceDir = Paths.get(source)

    val namespaceMatch = {
      namespace match {
        case Some(ns) =>
          val nsRegex = s"""^$source/$ns/.*""".r
          (path: String) =>
            nsRegex.findFirstIn(path).isDefined
        case _ =>
          (_: String) => true
      }
    }

    // find functionality.yaml files and parse as config
    val funFiles = find(sourceDir, (path, attrs) => {
      path.toString.endsWith("functionality.yaml") && attrs.isRegularFile && namespaceMatch(path.toString)
    })

    // read functionality + platforms according to
    val legacyConfigs = funFiles.map { file =>
      try {
        val _namespace = getNamespace(namespace, file)
        Left(Config.readSplitOrJoined(
          functionality = Some(file.toString),
          platform = platform,
          platformID = platformID,
          namespace = _namespace
        ))
      } catch {
        case e: PlatformNotFoundException => Right(e)
      }
    }

    // find *.vsh.* files and parse as config
    val scriptRegex = ".*\\.vsh\\.[^.]*$".r
    val scriptFiles = find(sourceDir, (path, attrs) => {
      scriptRegex.findFirstIn(path.toString.toLowerCase).isDefined &&
        attrs.isRegularFile &&
        namespaceMatch(path.toString)
    })
    val newConfigs = scriptFiles.map { file =>
      try {
        val _namespace = getNamespace(namespace, file)
        Left(Config.readSplitOrJoined(
          joined = Some(file.toString),
          platform = platform,
          platformID = platformID,
          namespace = _namespace
        ))
      } catch {
        case e: PlatformNotFoundException => Right(e)
      }
    }

    // merge configs
    legacyConfigs ::: newConfigs
  }
}
