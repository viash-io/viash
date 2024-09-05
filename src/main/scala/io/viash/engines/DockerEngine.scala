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

import io.viash.config.{Config, BuildInfo, Author}
import io.viash.engines.requirements.{Requirements, DockerRequirements}
import io.viash.helpers.{Escaper, Docker}
import io.viash.wrapper.BashWrapper
import io.viash.helpers.data_structures.listToOneOrMore

import io.viash.schemas._
import io.viash.helpers.DockerImageInfo

@description(
  """Run a Viash component on a Docker backend engine.
    |By specifying which dependencies your component needs, users will be able to build a docker container from scratch using the setup flag, or pull it from a docker repository.
    |""".stripMargin)
@example(
  """engines:
    |  - type: docker
    |    image: "bash:4.0"
    |    setup:
    |      - type: apt
    |        packages: [ curl ]
    |""".stripMargin,
  "yaml")
@subclass("docker")
final case class DockerEngine(
  @description("Name of the engine. As with all engines, you can give a engine a different name. By specifying `id: foo`, you can target this engine (only) by specifying `...` in any of the Viash commands.")
  @example("id: foo", "yaml")
  @default("docker")
  id: String = "docker",

  @description("The base container to start from. You can also add the tag here if you wish.")
  @example("image: \"bash:4.0\"", "yaml")
  image: String,

  @description("Name of a start container's [organization](https://docs.docker.com/docker-hub/orgs/).")
  @deprecated("Use the full container name in `image` instead.", "0.9.0", "0.10.0")
  organization: Option[String],

  @description("The URL to the a [custom Docker registry](https://docs.docker.com/registry/) where the start container is located.")
  @example("registry: https://my-docker-registry.org", "yaml")
  @deprecated("Use the full container name in `image` instead.", "0.9.0", "0.10.0")
  registry: Option[String] = None,

  @description("Specify a Docker image based on its tag.")
  @example("tag: 4.0", "yaml")
  @deprecated("Use the full container name in `image` instead.", "0.9.0", "0.10.0")
  tag: Option[String] = None,

  @description("If anything is specified in the setup section, running the `---setup` will result in an image with the name of `<target_image>:<version>`. If nothing is specified in the `setup` section, simply `image` will be used. Advanced usage only.")
  @example("target_image: myfoo", "yaml")
  target_image: Option[String] = None,

  @description("The organization set in the resulting image. Advanced usage only.")
  @example("target_organization: viash-io", "yaml")
  target_organization: Option[String] = None,

  @description("The package name set in the resulting image. Advanced usage only.")
  @example("target_package: tools", "yaml")
  target_package: Option[String] = None,

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
      |"""/*.stripMargin*/)
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
  cmd: Option[Either[String, List[String]]] = None,

  `type`: String = "docker"
) extends Engine {
  val hasSetup = true

  /**
   * Generate a Dockerfile for the container
   *
   * @param config The config
   * @param info The config info (available)
   * @param testing Whether or not this container is used as part of a `viash test`, in which the `test_setup` also needs to be included
   *
   * @return The Dockerfile as a string
   */
  def dockerFile(
    config: Config,
    info: Option[BuildInfo],
    testing: Boolean
  ): String = {
    /* Construct labels from metadata */

    // derive authors
    val authors = config.authors match {
      case Nil => None
      case aut: List[Author] => Some(aut.map(_.name).mkString(", "))
    }
    val opencontainers_image_authors = authors.map(aut => s"""org.opencontainers.image.authors="${Escaper(aut, quote = true)}"""").toList

    // define description
    val imageDescription = s""""Companion container for running component ${config.namespace.map(_ + " ").getOrElse("")}${config.name}""""
    val opencontainersImageDescription = List(s"org.opencontainers.image.description=$imageDescription")

    // define created
    val imageCreated = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").format(new Date())
    val opencontainersImageCreated = List(s"""org.opencontainers.image.created="$imageCreated"""")

    // define version
    val imageVersion = config.version.map(v => v.toString())
    val opencontainersImageVersion = imageVersion.map(v => s"""org.opencontainers.image.version="$v"""").toList

    // define image version
    val imageRevision = info.flatMap(_.git_commit)
    val opencontainersImageRevision = imageRevision.map(rev => s"""org.opencontainers.image.revision="$rev"""").toList

    // define image source
    //   if no target_image_source is defined, translate
    //   git@github.com:viash-io/viash.git -> https://github.com/viash-io/viash.git
    val imageSource = target_image_source
      .orElse(config.links.repository)
      .orElse(info.flatMap(
        _.git_remote.map(
          _.replaceAll(":([^/])", "/$1")
            .replace("ssh//", "")
            .replace("git@", "https://")
        )
      ))
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
    // TODO: once registry, organization and tag are removed, `fromImageInfo.toString()` is always equal to `image` so it can be removed.
    val fromImageInfo = Docker.getImageInfo(
      name = Some(image),
      registry = registry,
      organization = organization,
      tag = tag
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

  def getTargetIdentifier(config: Config): DockerImageInfo = {

    val targetRegistryWithFallback = target_registry.orElse(config.links.docker_registry).filter(_.nonEmpty)
    val targetOrganizationWithFallback = target_organization.orElse(config.package_config.flatMap(_.organization)).filter(_.nonEmpty)
    val targetPackageNameWithFallback = target_package.orElse(config.package_config.flatMap(_.name)).filter(_.nonEmpty)

    // TODO: Once registry, organization and tag are removed, and `fromImageInfo` doesn't use this function anymore, make `config` and `namespaceSeparator` required.
    Docker.getImageInfo(
      config = Some(config),
      engineId = Some(id),
      registry = targetRegistryWithFallback,
      organization = targetOrganizationWithFallback,
      `package` = targetPackageNameWithFallback,
      name = target_image,
      tag = target_tag,
      namespaceSeparator = Some(namespace_separator)
    )
  }
}
