/*
 * Copyright (C) 2020  Data Intuitive
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.viash.engines

import java.text.SimpleDateFormat
import java.util.Date

import io.viash.config.{Info => ConfigInfo}
import io.viash.functionality.Functionality
import io.viash.functionality.Author

// todo: move these requirements to .engines
import io.viash.platforms.requirements.Requirements
import io.viash.platforms.requirements.DockerRequirements

import io.viash.helpers.Escaper
import io.viash.helpers.Docker
import io.viash.wrapper.BashWrapper

import io.viash.schemas._

final case class DockerEngine(
  @description("Name of the engine. As with all engines, you can give a engine a different name. By specifying `id: foo`, you can target this engine (only) by specifying `...` in any of the Viash commands.")
  @example("id: foo", "yaml")
  @default("docker")
  id: String = "docker",

  @description("The base container to start from. You can also add the tag here if you wish.")
  @example("image: \"bash:4.0\"", "yaml")
  image: String,

  @description("Name of a container's [organization](https://docs.docker.com/docker-hub/orgs/).")
  organization: Option[String],

  @description("The URL to the a [custom Docker registry](https://docs.docker.com/registry/)")
  @example("registry: https://my-docker-registry.org", "yaml")
  registry: Option[String] = None,

  @description("Specify a Docker image based on its tag.")
  @example("tag: 4.0", "yaml")
  tag: Option[String] = None,
  
  @description("If anything is specified in the setup section, running the `---setup` will result in an image with the name of `<target_image>:<version>`. If nothing is specified in the `setup` section, simply `image` will be used. Advanced usage only.")
  @example("target_image: myfoo", "yaml")
  target_image: Option[String] = None,

  @description("The organization set in the resulting image. Advanced usage only.")
  @example("target_organization: viash-io", "yaml")
  target_organization: Option[String] = None,

  @description("The URL where the resulting image will be pushed to. Advanced usage only.")
  @example("target_registry: https://my-docker-registry.org", "yaml")
  target_registry: Option[String] = None,

  @description("The tag the resulting image gets. Advanced usage only.")
  @example("target_tag: 0.5.0", "yaml")
  target_tag: Option[String] = None,

  @description("The separator between the namespace and the name of the component, used for determining the image name. Default: `\"/\"`.")
  @example("namespace_separator: \"_\"", "yaml")
  @default("/")
  namespace_separator: String = "/",

  @description("The source of the target image. This is used for defining labels in the dockerfile.")
  @example("target_image_source: https://github.com/foo/bar", "yaml")
  target_image_source: Option[String] = None,

  @description(
    """A list of requirements for installing the following types of packages:
      |
      | - @[apt](apt_req)
      | - @[apk](apk_req)
      | - @[Docker setup instructions](docker_req)
      | - @[JavaScript](javascript_req)
      | - @[Python](python_req)
      | - @[R](r_req)
      | - @[Ruby](ruby_req)
      | - @[yum](yum_req)
      |
      |The order in which these dependencies are specified determines the order in which they will be installed.
      |""".stripMargin)
  @default("Empty")
  setup: List[Requirements] = Nil,

  @description("Additional requirements specific for running unit tests.")
  @since("Viash 0.5.13")
  @default("Empty")
  test_setup: List[Requirements] = Nil,

  @description("Override the entrypoint of the base container. Default set `ENTRYPOINT []`.")
  @exampleWithDescription("entrypoint: ", "yaml", "Disable the default override.")
  @exampleWithDescription("""entrypoint: ["top", "-b"]""", "yaml", "Entrypoint of the container in the exec format, which is the prefered form.")
  @exampleWithDescription("""entrypoint: "top -b"""", "yaml", "Entrypoint of the container in the shell format.")
  @since("Viash 0.7.4")
  @default("[]")
  entrypoint: Option[Either[String, List[String]]] = Some(Right(Nil)),

  @description("Set the default command being executed when running the Docker container.")
  @exampleWithDescription("""cmd: ["echo", "$HOME"]""", "yaml", "Set CMD using the exec format, which is the prefered form.")
  @exampleWithDescription("""cmd: "echo $HOME"""", "yaml", "Set CMD using the shell format.")
  @since("Viash 0.7.4")
  cmd: Option[Either[String, List[String]]] = None
) extends Engine {
  val `type` = "docker"

  /**
   * Generate a Dockerfile for the container
   * 
   * @param functionality The functionality
   * @param info The config info (available)
   * @param testing Whether or not this container is used as part of a `viash test`, in which the `test_setup` also needs to be included
   * 
   * @return The Dockerfile as a string
   */
  def dockerFile(
    functionality: Functionality,
    info: Option[ConfigInfo],
    testing: Boolean
  ): String = {
    /* Construct labels from metadata */
    
    // derive authors
    val authors = functionality.authors match {
      case Nil => None
      case aut: List[Author] => Some(aut.map(_.name).mkString(", "))
    }
    val opencontainers_image_authors = authors.map(aut => s"""org.opencontainers.image.authors="${Escaper(aut, quote = true)}"""").toList

    // define description
    val imageDescription = s""""Companion container for running component ${functionality.namespace.map(_ + " ").getOrElse("")}${functionality.name}""""
    val opencontainersImageDescription = List(s"org.opencontainers.image.description=$imageDescription")

    // define created
    val imageCreated = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").format(new Date())
    val opencontainersImageCreated = List(s"""org.opencontainers.image.created="$imageCreated"""")

    // define version
    val imageVersion = functionality.version.map(v => v.toString())
    val opencontainersImageVersion = imageVersion.map(v => s"""org.opencontainers.image.version="$v"""").toList

    // define image version
    val imageRevision = info.flatMap(_.git_commit)
    val opencontainersImageRevision = imageRevision.map(rev => s"""org.opencontainers.image.revision="$rev"""").toList

    // define image source
    //   if no target_image_source is defined, translate
    //   git@github.com:viash-io/viash.git -> https://github.com/viash-io/viash.git
    val imageSource = (target_image_source, info) match {
      case (Some(targetImageSource), _) => Some(targetImageSource)
      case (None, Some(configInfo)) =>
        configInfo.git_remote.map(url =>
          url.replaceAll(":([^/])", "/$1")
            .replace("ssh//", "")
            .replace("git@", "https://")
        )
      case _ => None
    }
    val opencontainersImageSource = imageSource.map(src => s"""org.opencontainers.image.source="${Escaper(src, quote = true)}"""").toList
    
    val labelReq = DockerRequirements(label = 
      opencontainers_image_authors :::
        opencontainersImageDescription :::
        opencontainersImageCreated :::
        opencontainersImageSource :::
        opencontainersImageRevision :::
        opencontainersImageVersion
    )

    /* Fetch from image name */
    val fromImageInfo = Docker.getImageInfo(
      name = Some(image),
      registry = registry,
      organization = organization,
      tag = tag.map(_.toString),
      namespaceSeparator = namespace_separator
    )

    /* Construct Dockerfile */
    val requirements = setup ::: { if (testing) test_setup else Nil } ::: List(labelReq)

    val runCommands = requirements.flatMap(_.dockerCommands)

    val entrypointStr = Docker.listifyOneOrMore(entrypoint).map(s => s"\nENTRYPOINT $s").getOrElse("")
    val cmdStr = Docker.listifyOneOrMore(cmd).map(s => s"\nCMD $s").getOrElse("")

    s"""FROM $fromImageInfo$entrypointStr$cmdStr 
        |${runCommands.mkString("\n")}
        |""".stripMargin           
  }

  def getTargetIdentifier(functionality: Functionality): String = {
    val targetImageInfo = Docker.getImageInfo(
      functionality = Some(functionality),
      engineId = Some(id),
      registry = target_registry,
      organization = target_organization,
      name = target_image,
      tag = target_tag.map(_.toString),
      namespaceSeparator = namespace_separator
    )

    targetImageInfo.toString
  }
}
