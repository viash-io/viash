package com.dataintuitive.viash.config

import com.dataintuitive.viash.functionality._
import com.dataintuitive.viash.platforms._
import com.dataintuitive.viash.helpers.IO
import java.net.URI
import io.circe.yaml.parser
import com.dataintuitive.viash.functionality.resources._
import com.dataintuitive.viash.helpers.{Git, GitInfo}
import java.io.File

case class Config(
  functionality: Functionality,
  platform: Option[Platform] = None,
  platforms: List[Platform] = Nil,
  info: Option[Info] = None
)

object Config {
  def parse(uri: URI): Config = {
    val str = IO.read(uri)
    parse(str, uri)
  }

  def parse(yamlText: String, uri: URI): Config = {
    val config = parser.parse(yamlText)
      .fold(throw _, _.as[Config])
      .fold(throw _, identity)

    val fun = config.functionality

    // make paths absolute
    val resources = fun.resources.getOrElse(Nil).map(Functionality.makeResourcePathAbsolute(_, uri))
    val tests = fun.tests.getOrElse(Nil).map(Functionality.makeResourcePathAbsolute(_, uri))

    // copy resources with updated paths into config and return
    config.copy(
      functionality = fun.copy(
        resources = Some(resources),
        tests = Some(tests)
      )
    )
  }

  def read(path: String): Config = {
    val uri = IO.uri(path)

    val str = IO.read(uri)
    val uris = uri.toString
    val extension = uris.substring(uris.lastIndexOf(".") + 1).toLowerCase()

    // detect whether a script (with joined header) was passed or a joined yaml
    // using the extension
    val (yaml, optScript) =
      if (extension == "yml" || extension == "yaml") {
        (str, None)
      } else {
        val scriptObj = Script.fromExt(extension)
        val commentStr = scriptObj.commentStr + "'"
        val headerComm = commentStr + " "
        assert(
          str.contains(s"$commentStr functionality:"),
          message = s"""Script should contain a functionality header: "$commentStr functionality: <...>""""
        )

        val (header, body) = str.split("\n").partition(_.startsWith(headerComm))
        val yaml = header.map(s => s.drop(3)).mkString("\n")
        val code = commentStr + " VIASH START\n" + commentStr + " VIASH END\n" + body.mkString("\n")

        val script = scriptObj(name = Some("viash_main."), text = Some(code))
        (yaml, Some(script))
      }

    // read config
    val config = parse(yaml, uri)

    config.copy(
      functionality = config.functionality.copy(
        resources = Some(optScript.toList ::: config.functionality.resources.getOrElse(Nil))
      )
    )
  }

  class PlatformNotFoundException(val config: Config, val platform: String) extends RuntimeException(s"Platform platform could not be found")

  def readSplitOrJoined(
    joined: Option[String] = None,
    functionality: Option[String] = None,
    platform: Option[String] = None,
    platformID: Option[String] = None,
    modifyFun: Boolean = true,
    namespace: Option[String] = None
  ): Config = {
    // read the component if passed, else read the functionality
    assert(
      joined.isEmpty != functionality.isEmpty,
      message = "Either functionality or joined need to be specified!"
    )

    // construct info object
    val configPre = {
      if (joined.isDefined) {
        read(joined.get)
      } else {
        Config(
          functionality = Functionality.read(functionality.get)
        )
      }
    }.copy(
      info = Some(Info(
        functionality_path = functionality,
        platform_path = platform,
        platform_id = platformID,
        joined_path = joined,
        viash_version = Some(com.dataintuitive.viash.Main.version)
      ))
    )

    val keepExplicitNamespace =
      if (configPre.functionality.namespace != None)
        configPre.functionality.namespace
      else
        namespace

    val config = configPre.copy(functionality = configPre.functionality.copy(namespace = keepExplicitNamespace))


    // get the platform
    // * if a platform yaml is passed, use that
    // * else if a platform id is passed, look up the platform in the platforms list
    // * else if a platform is already defined in the config, use that
    // * else if platforms is a non-empty list, use the first platform
    // * else use the native platform
    val pl =
      if (platform.isDefined) {
        Platform.parse(IO.uri(platform.get))
      } else if (platformID.isDefined) {
        val pid = platformID.get
        if (joined.isDefined) {
          // if input file is a joined config
          val platformNames = config.platforms.map(_.id)
          if (!platformNames.contains(pid)) {
            throw new PlatformNotFoundException(config, pid)
          }
          config.platforms(platformNames.indexOf(pid))
        } else {
          val funRegex = "[^/]*.yaml$".r
          // if input file is a functionality yaml,
          // check if platform_*.yaml file exists
          val platPath = funRegex.replaceFirstIn(functionality.get, "platform_" + pid + ".yaml")
          val uri = IO.uri(platPath)
          val platform =
            try {
              Some(Platform.parse(uri))
            } catch {
              case _: Throwable => None
            }

          if (platform.isEmpty) {
            throw new PlatformNotFoundException(config, platPath)
          }

          platform.get
        }
      } else if (config.platform.isDefined) {
        config.platform.get
      } else if (config.platforms.nonEmpty) {
        config.platforms.head
      } else {
        NativePlatform()
      }

    // gather git info of functionality git
    val path = new File(functionality.getOrElse(joined.getOrElse(""))).getParentFile
    val GitInfo(_, rgr, gc) = Git.getInfo(path)

    config.copy(
      functionality =
        if (modifyFun) {
          pl.modifyFunctionality(config.functionality)
        } else {
          config.functionality
        },
      platform = Some(pl),
      info = config.info.map(_.copy(
        git_commit = gc,
        git_remote = rgr
      ))
    )
  }

}
