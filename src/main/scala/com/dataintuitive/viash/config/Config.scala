package com.dataintuitive.viash.config

import com.dataintuitive.viash.functionality._
import com.dataintuitive.viash.platforms._
import com.dataintuitive.viash.helpers.IOHelper
import java.net.URI
import io.circe.yaml.parser
import com.dataintuitive.viash.functionality.resources._
import com.dataintuitive.viash.helpers.Git
import java.io.File

case class Config(
  functionality: Functionality,
  platform: Option[Platform] = None,
  platforms: List[Platform] = Nil,
  info: Option[Info] = None
)

object Config {
  def parse(uri: URI): Config = {
    val str = IOHelper.read(uri)
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

  def readComponent(path: String) = {
    val uri = IOHelper.uri(path)

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
    val config = parse(yaml, uri)

    config.copy(
      functionality = config.functionality.copy(
         resources = Some(componentScript.toList ::: config.functionality.resources.getOrElse(Nil))
      )
    )
  }

  def read(
    component: Option[String],
    functionality: Option[String],
    platform: Option[String],
    platformID: Option[String],
    modifyFun: Boolean = true
  ): Config = {
    // read the component if passed, else read the functionality
    assert(
      component.isEmpty != functionality.isEmpty,
      message = "Either functionality or component need to be specified!"
    )
    val config =
      if (component.isDefined) {
        readComponent(component.get)
      } else {
        Config(
          functionality = Functionality.read(functionality.get)
        )
      }

    // get the platform
    // * if a platform yaml is passed, use that
    // * else if a platform id is passed, look up the platform in the platforms list
    // * else if a platform is already defined in the config, use that
    // * else if platforms is a non-empty list, use the first platform
    // * else use the native platform
    val pl =
      if (platform.isDefined) {
        Platform.parse(IOHelper.uri(platform.get))
      } else if (platformID.isDefined) {
        val pid = platformID.get
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

    // gather git info of functionality git
    val path = new File(functionality.getOrElse(component.getOrElse(""))).getParentFile
    val (_, rgr, gc) = Git.getInfo(path.getParentFile)

    // construct info object
    val info = Info(
      functionality_path = functionality,
      platform_path = platform,
      platform_id = platformID,
      executable_path = component,
      output_path = None,
      viash_version = Some(com.dataintuitive.viash.Main.version),
      git_commit = gc,
      git_remote = rgr
    )

    // modify the functionality using the platform
    config.copy(
      functionality =
        if (modifyFun) {
          pl.modifyFunctionality(config.functionality)
        } else {
          config.functionality
        },
      platform = Some(pl),
      info = Some(info)
    )
  }

}