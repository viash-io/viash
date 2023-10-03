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

package io.viash.platforms

import io.viash.schemas._
import io.viash.helpers.data_structures._
import io.viash.runners.executable.{IfNeedBePullElseCachedBuild, DockerSetupStrategy}
import io.viash.engines.requirements.Requirements
import io.viash.platforms.docker.{DockerResolveVolume, Automatic}


@description(
  """Run a Viash component on a Docker backend platform.
    |By specifying which dependencies your component needs, users will be able to build a docker container from scratch using the setup flag, or pull it from a docker repository.
    |""".stripMargin)
@example(
  """platforms:
    |  - type: docker
    |    image: "bash:4.0"
    |    setup:
    |      - type: apt
    |        packages: [ curl ]
    |""".stripMargin,
  "yaml")
@deprecated("Use 'engines' and 'runners' instead.", "0.9.0", "0.10.0")
@subclass("docker")
case class DockerPlatform(
  @description("As with all platforms, you can give a platform a different name. By specifying `id: foo`, you can target this platform (only) by specifying `-p foo` in any of the Viash commands.")
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

  @description("Enables or disables automatic volume mapping. Enabled when set to `Automatic` or disabled when set to `Manual`. Default: `Automatic`.")
  @default("Automatic")
  resolve_volume: DockerResolveVolume = Automatic,

  @description("In Linux, files created by a Docker container will be owned by `root`. With `chown: true`, Viash will automatically change the ownership of output files (arguments with `type: file` and `direction: output`) to the user running the Viash command after execution of the component. Default value: `true`.")
  @example("chown: false", "yaml")
  @default("True")
  @removed("Compability not provided with the Runners functionality.", "0.8.0", "0.8.0")
  chown: Boolean = true,

  @description("A list of enabled ports. This doesn't change the Dockerfile but gets added as a command-line argument at runtime.")
  @example(
    """port:
      |  - 80
      |  - 8080
      |""".stripMargin,
      "yaml")
  @default("Empty")
  port: OneOrMore[String] = Nil,

  @description("The working directory when starting the container. This doesn't change the Dockerfile but gets added as a command-line argument at runtime.")
  @example("workdir: /home/user", "yaml")
  workdir: Option[String] = None,

  @description(
    """The Docker setup strategy to use when building a container.
      +
      +| Strategy | Description |
      +|-----|----------|
      +| `alwaysbuild` / `build` / `b` | Always build the image from the dockerfile. This is the default setup strategy.
      +| `alwayscachedbuild` / `cachedbuild` / `cb` | Always build the image from the dockerfile, with caching enabled.
      +| `ifneedbebuild` |  Build the image if it does not exist locally.
      +| `ifneedbecachedbuild` | Build the image with caching enabled if it does not exist locally, with caching enabled.
      +| `alwayspull` / `pull` / `p` |  Try to pull the container from [Docker Hub](https://hub.docker.com) or the @[specified docker registry](docker_registry).
      +| `alwayspullelsebuild` / `pullelsebuild` |  Try to pull the image from a registry and build it if it doesn't exist.
      +| `alwayspullelsecachedbuild` / `pullelsecachedbuild` |  Try to pull the image from a registry and build it with caching if it doesn't exist.
      +| `ifneedbepull` |  If the image does not exist locally, pull the image.
      +| `ifneedbepullelsebuild` |  If the image does not exist locally, pull the image. If the image does exist, build it.
      +| `ifneedbepullelsecachedbuild` | If the image does not exist locally, pull the image. If the image does exist, build it with caching enabled.
      +| `push` | Push the container to [Docker Hub](https://hub.docker.com)  or the @[specified docker registry](docker_registry).
      +| `pushifnotpresent` | Push the container to [Docker Hub](https://hub.docker.com) or the @[specified docker registry](docker_registry) if the @[tag](docker_tag) does not exist yet.
      +| `donothing` / `meh` | Do not build or pull anything.
      +
      +""".stripMargin('+'))
  @example("setup_strategy: alwaysbuild", "yaml")
  @default("ifneedbepullelsecachedbuild")
  setup_strategy: DockerSetupStrategy = IfNeedBePullElseCachedBuild,

  @description("Add [docker run](https://docs.docker.com/engine/reference/run/) arguments.")
  @default("Empty")
  run_args: OneOrMore[String] = Nil,

  @description("The source of the target image. This is used for defining labels in the dockerfile.")
  @example("target_image_source: https://github.com/foo/bar", "yaml")
  target_image_source: Option[String] = None,
  `type`: String = "docker",

  // setup variables
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

) extends Platform