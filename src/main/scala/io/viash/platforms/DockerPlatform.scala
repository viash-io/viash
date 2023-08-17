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

import java.util.Date
import java.text.SimpleDateFormat

import io.viash.config.Config
import io.viash.functionality._
import io.viash.functionality.arguments._
import io.viash.functionality.resources._
import io.viash.platforms.requirements._
import io.viash.helpers.{Bash, Docker}
import io.viash.wrapper.{BashWrapper, BashWrapperMods}
import io.viash.platforms.docker._
import io.viash.helpers.data_structures._
import io.viash.config.Info
import io.viash.schemas._
import io.viash.helpers.Escaper

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

) extends Container {
  @internalFunctionality
  override val hasSetup = true

  override val requirements: List[Requirements] = {
    // workaround for making sure that every docker platform creates a new container
    if (setup.isEmpty) {
      List(DockerRequirements(
        run = List(":")
      ))
    } else {
      setup
    }
  }


  def modifyFunctionality(config: Config, testing: Boolean): Functionality = {
    val functionality = config.functionality
    // collect docker args
    val dockerArgs = "-i --rm" +
      port.map(" -p " + _).mkString +
      run_args.map(" " + _).mkString

    // create setup
    val (effectiveID, setupMods) = processDockerSetup(functionality, config.info, testing)

    // generate automount code
    val dmVol = processDockerVolumes(functionality)

    // generate installationcheck code
    val dmDockerCheck = addDockerCheck()

    // add ---debug flag
    val debuggor = s"""docker run --entrypoint=bash $dockerArgs -v "$$(pwd)":/pwd --workdir /pwd -t '$effectiveID'"""
    val dmDebug = addDockerDebug(debuggor)

    // add ---chown flag
    val dmChown = addDockerChown(functionality, dockerArgs, dmVol.extraParams, effectiveID)

    // process cpus and memory_b
    val dmReqs = addComputationalRequirements(functionality)

    // compile modifications
    val dm = dmDockerCheck ++ setupMods ++ dmVol ++ dmDebug ++ dmChown ++ dmReqs

    // make commands
    val entrypointStr = functionality.mainScript match {
      case Some(_: Executable) => "--entrypoint='' "
      case _ => "--entrypoint=bash "
    }
    val workdirStr = workdir.map("--workdir " + _ + " ").getOrElse("")
    val executor = s"""eval docker run $entrypointStr$workdirStr$dockerArgs${dm.extraParams} $effectiveID"""

    // add extra arguments to the functionality file for each of the volumes
    val fun2 = functionality.copy(
      arguments = functionality.arguments ::: dm.inputs
    )

    // create new bash script
    val bashScript = BashScript(
      dest = Some(functionality.name),
      text = Some(BashWrapper.wrapScript(
        executor = executor,
        functionality = fun2,
        mods = dm
      ))
    )

    fun2.copy(
      resources = bashScript :: fun2.additionalResources
    )
  }

  private def processDockerSetup(functionality: Functionality, info: Option[Info], testing: Boolean) = {
    // construct labels from metadata
    val opencontainers_image_authors = functionality.authors match {
      case Nil => None
      case aut: List[Author] => Some(aut.map(_.name).mkString(", "))
    }
    // if no target_image_source is defined,
    // translate git@github.com:viash-io/viash.git -> https://github.com/viash-io/viash.git
    val opencontainers_image_source = (target_image_source, info) match {
      case (Some(tis), _) => Some(tis)
      case (None, Some(i)) =>
        i.git_remote.map(url => url.replaceAll(":([^/])", "/$1").replace("ssh//", "").replace("git@", "https://"))
      case _ => None
    }
    val opencontainers_image_revision = info.flatMap(_.git_commit)
    val opencontainers_image_version = functionality.version.map(v => v.toString())
    val opencontainers_image_description = s""""Companion container for running component ${functionality.namespace.map(_ + " ").getOrElse("")}${functionality.name}""""
    val opencontainers_image_created = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").format(new Date())

    val authors = opencontainers_image_authors.map(aut => s"""org.opencontainers.image.authors="${Escaper(aut, quote = true)}"""").toList
    val descr = List(s"org.opencontainers.image.description=$opencontainers_image_description")
    val imageSource = opencontainers_image_source.map(src => s"""org.opencontainers.image.source="${Escaper(src, quote = true)}"""").toList
    val created = List(s"""org.opencontainers.image.created="$opencontainers_image_created"""")
    val revision = opencontainers_image_revision.map(rev => s"""org.opencontainers.image.revision="$rev"""").toList
    val version = opencontainers_image_version.map(v => s"""org.opencontainers.image.version="$v"""").toList
    val labelReq = DockerRequirements(label = authors ::: descr ::: created ::: imageSource ::: revision ::: version)

    val setupRequirements = testing match {
      case true => test_setup
      case _ => Nil
    }

    val requirements2 = requirements ::: setupRequirements ::: List(labelReq)

    // get dependencies
    val runCommands = requirements2.flatMap(_.dockerCommands)

    // don't draw defaults from functionality for the from image
    val fromImageInfo = Docker.getImageInfo(
      name = Some(image),
      registry = registry,
      organization = organization,
      tag = tag.map(_.toString),
      namespaceSeparator = namespace_separator
    )
    val targetImageInfo = Docker.getImageInfo(
      functionality = Some(functionality),
      registry = target_registry,
      organization = target_organization,
      name = target_image,
      tag = target_tag.map(_.toString),
      namespaceSeparator = namespace_separator
    )

    // if there are no requirements, simply use the specified image
    val effectiveID =
      if (runCommands.isEmpty) {
        fromImageInfo.toString
      } else {
        targetImageInfo.toString
      }

    def entrypointCmdMap(values: Option[Either[String, List[String]]]): Option[String] =
      values match {
        case None => None
        case Some(Left(s)) => Some(s)
        case Some(Right(list)) if list.isEmpty => Some("[]")
        case Some(Right(list)) => Some(list.mkString("[\"", "\",\"", "\"]"))
      }

    val entrypointStr = entrypointCmdMap(entrypoint).map(s => s"ENTRYPOINT $s\n").getOrElse("")
    val cmdStr = entrypointCmdMap(cmd).map(s => s"CMD $s\n").getOrElse("")

    // if no extra dependencies are needed, the provided image can just be used,
    // otherwise need to construct a separate docker container
    val (viashDockerFile, viashDockerBuild) =
      if (runCommands.isEmpty) {
        ("  :", "  ViashDockerPull $1")
      } else {
        val dockerFile =
          s"""FROM ${fromImageInfo.toString}
             |
             |$entrypointStr
             |$cmdStr 
             |${runCommands.mkString("\n")}
             |""".stripMargin           

        val dockerRequirements =
          requirements.flatMap {
            case d: DockerRequirements => Some(d)
            case _ => None
          }
        val buildArgs = dockerRequirements.map(_.build_args.map(" --build-arg " + _).mkString).mkString("")

        val vdf =
          s"""  cat << 'VIASHDOCKER'
             |$dockerFile
             |VIASHDOCKER""".stripMargin

        val vdb =
          s"""  # create temporary directory to store dockerfile & optional resources in
             |  tmpdir=$$(mktemp -d "$$VIASH_META_TEMP_DIR/dockerbuild-${functionality.name}-XXXXXX")
             |  dockerfile="$$tmpdir/Dockerfile"
             |  function clean_up {
             |    rm -rf "$$tmpdir"
             |  }
             |  trap clean_up EXIT
             |
             |  # store dockerfile and resources
             |  ViashDockerfile > $$dockerfile
             |
             |  # Build the container
             |  ViashNotice "Building container '$$1' with Dockerfile"
             |  ViashInfo "Running 'docker build -t $$@$buildArgs $$VIASH_META_RESOURCES_DIR -f $$dockerfile'"
             |  save=$$-; set +e
             |  if [ $$${BashWrapper.var_verbosity} -ge $$VIASH_LOGCODE_INFO ]; then
             |    docker build -t $$@$buildArgs $$VIASH_META_RESOURCES_DIR -f $$dockerfile
             |  else
             |    docker build -t $$@$buildArgs $$VIASH_META_RESOURCES_DIR -f $$dockerfile &> $$tmpdir/docker_build.log
             |  fi
             |  out=$$?
             |  [[ $$save =~ e ]] && set -e
             |  if [ $$out -ne 0 ]; then
             |    ViashError "Error occurred while building container '$$1'"
             |    if [ $$${BashWrapper.var_verbosity} -lt $$VIASH_LOGCODE_INFO ]; then
             |      ViashError "Transcript: --------------------------------"
             |      cat "$$tmpdir/docker_build.log"
             |      ViashError "End of transcript --------------------------"
             |    fi
             |    exit 1
             |  fi""".stripMargin

        (vdf, vdb)
      }
    
    // get list of all the commands that should be available in the container
    val commandsToCheck = functionality.requirements.commands ::: List("bash")
    val commandsToCheckStr = commandsToCheck.mkString("'", "' '", "'")
      
    val preParse =
      s"""
         |${Bash.ViashDockerFuns}
         |
         |# ViashDockerFile: print the dockerfile to stdout
         |# return : dockerfile required to run this component
         |# examples:
         |#   ViashDockerFile
         |function ViashDockerfile {
         |$viashDockerFile
         |}
         |
         |# ViashDockerBuild: build a docker container
         |# $$1              : image identifier with format `[registry/]image[:tag]`
         |# exit code $$?    : whether or not the image was built
         |function ViashDockerBuild {
         |$viashDockerBuild
         |  ViashDockerCheckCommands "$$1" $commandsToCheckStr
         |}""".stripMargin

    val parsers =
      s"""
         |        ---setup)
         |            VIASH_MODE='docker_setup'
         |            VIASH_DOCKER_SETUP_STRATEGY="$$2"
         |            shift 1
         |            ;;
         |        ---setup=*)
         |            VIASH_MODE='docker_setup'
         |            VIASH_DOCKER_SETUP_STRATEGY="$$(ViashRemoveFlags "$$1")"
         |            shift 2
         |            ;;
         |        ---dockerfile)
         |            ViashDockerfile
         |            exit 0
         |            ;;""".stripMargin

    val postParse =
      s"""
         |if [ $$VIASH_MODE == "docker_setup" ]; then
         |  ViashDockerSetup '$effectiveID' "$$VIASH_DOCKER_SETUP_STRATEGY"
         |  exit 0
         |fi
         |ViashDockerSetup '$effectiveID' ${IfNeedBePullElseCachedBuild.id}""".stripMargin

    val mods = BashWrapperMods(
      preParse = preParse,
      parsers = parsers,
      postParse = postParse
    )

    (effectiveID, mods)
  }

  private def processDockerVolumes(functionality: Functionality) = {
    val args = functionality.getArgumentLikes(includeMeta = true)
    val extraMountsVar = "VIASH_EXTRA_MOUNTS"

    val preParse =
      s"""
         |${Bash.ViashAbsolutePath}
         |${Bash.ViashAutodetectMount}
         |${Bash.ViashExtractFlags}
         |# initialise variables
         |VIASH_EXTRA_MOUNTS=()""".stripMargin


    val parsers =
      s"""
         |        ---v|---volume)
         |            VIASH_EXTRA_MOUNTS+=("--volume='$$2'")
         |            shift 2
         |            ;;
         |        ---volume=*)
         |            VIASH_EXTRA_MOUNTS+=("--volume='$$(ViashRemoveFlags "$$2")'")
         |            shift 1
         |            ;;""".stripMargin

    val preRun =
      if (resolve_volume == Automatic) {
        val detectMounts = args.flatMap {
          case arg: FileArgument if arg.multiple =>
            // resolve arguments with multiplicity different from singular args
            val viash_temp = "VIASH_TEST_" + arg.plainName.toUpperCase()
            val chownIfOutput = if (arg.direction == Output) "\n    VIASH_CHOWN_VARS+=( \"$var\" )" else ""
            Some(
              s"""
                  |if [ ! -z "$$${arg.VIASH_PAR}" ]; then
                  |  $viash_temp=()
                  |  IFS='${Bash.escapeString(arg.multiple_sep, quote = true)}'
                  |  for var in $$${arg.VIASH_PAR}; do
                  |    unset IFS
                  |    VIASH_EXTRA_MOUNTS+=( "$$(ViashAutodetectMountArg "$$var")" )
                  |    var=$$(ViashAutodetectMount "$$var")
                  |    $viash_temp+=( "$$var" )$chownIfOutput
                  |  done
                  |  ${arg.VIASH_PAR}=$$(IFS='${Bash.escapeString(arg.multiple_sep, quote = true)}' ; echo "$${$viash_temp[*]}")
                  |fi""".stripMargin)
          case arg: FileArgument =>
            val chownIfOutput = if (arg.direction == Output) "\n  VIASH_CHOWN_VARS+=( \"$" + arg.VIASH_PAR + "\" )" else ""
            Some(
              s"""
                  |if [ ! -z "$$${arg.VIASH_PAR}" ]; then
                  |  VIASH_EXTRA_MOUNTS+=( "$$(ViashAutodetectMountArg "$$${arg.VIASH_PAR}")" )
                  |  ${arg.VIASH_PAR}=$$(ViashAutodetectMount "$$${arg.VIASH_PAR}")$chownIfOutput
                  |fi""".stripMargin)
          case _ => None
        }
        
        if (detectMounts.isEmpty) {
          ""
        } else {
          f"""
          |
          |# detect volumes from file arguments
          |VIASH_CHOWN_VARS=()${detectMounts.mkString("")}
          |
          |# get unique mounts
          |VIASH_UNIQUE_MOUNTS=($$(for val in "$${VIASH_EXTRA_MOUNTS[@]}"; do echo "$$val"; done | sort -u))
          |""".stripMargin
        }
      } else {
        ""
      }
    
    val postRun =
      if (resolve_volume == Automatic) {
        val stripAutomounts = args.flatMap {
          case arg: FileArgument if arg.multiple =>
            // resolve arguments with multiplicity different from singular args
            val viash_temp = "VIASH_TEST_" + arg.plainName.toUpperCase()
            Some(
              s"""
                  |if [ ! -z "$$${arg.VIASH_PAR}" ]; then
                  |  unset $viash_temp
                  |  IFS='${Bash.escapeString(arg.multiple_sep, quote = true)}'
                  |  for var in $$${arg.VIASH_PAR}; do
                  |    unset IFS
                  |    ${BashWrapper.store("ViashStripAutomount", viash_temp, "\"$(ViashStripAutomount \"$var\")\"", Some(arg.multiple_sep)).mkString("\n    ")}
                  |  done
                  |  ${arg.VIASH_PAR}="$$$viash_temp"
                  |fi""".stripMargin)
          case arg: FileArgument =>
            Some(
              s"""
                  |if [ ! -z "$$${arg.VIASH_PAR}" ]; then
                  |  ${arg.VIASH_PAR}=$$(ViashStripAutomount "$$${arg.VIASH_PAR}")
                  |fi""".stripMargin)
          case _ => None
        }
        
        if (stripAutomounts.isEmpty) {
          ""
        } else {
          "\n# strip viash automount from file paths" + stripAutomounts.mkString("")
        }
      } else {
        ""
      }

    val extraParams = s" $${VIASH_UNIQUE_MOUNTS[@]}"

    BashWrapperMods(
      preParse = preParse,
      parsers = parsers,
      preRun = preRun,
      postRun = postRun,
      extraParams = extraParams
    )
  }

  private def addDockerCheck() = {
    val postParse =
      s"""
         |ViashDockerInstallationCheck""".stripMargin

    BashWrapperMods(
      postParse = postParse
    )
  }

  private def addDockerDebug(debugCommand: String) = {
    val parsers =
      s"""
         |        ---debug)
         |            VIASH_MODE='docker_debug'
         |            shift 1
         |            ;;""".stripMargin

    val postParse =
      s"""
         |if [ $$VIASH_MODE == "docker_debug" ]; then
         |  ViashNotice "+ $debugCommand"
         |  $debugCommand
         |  exit 0
         |fi""".stripMargin

    BashWrapperMods(
      parsers = parsers,
      postParse = postParse
    )
  }

  private def addDockerChown(
    functionality: Functionality,
    dockerArgs: String,
    volExtraParams: String,
    fullImageID: String,
  ) = {
    val preRun =
      if (chown) {
        s"""
           |# change file ownership
           |function ViashPerformChown {
           |  if (( $${#VIASH_CHOWN_VARS[@]} )); then
           |    set +e
           |    eval docker run --entrypoint=chown $dockerArgs$volExtraParams $fullImageID "$$(id -u):$$(id -g)" --silent --recursive $${VIASH_CHOWN_VARS[@]}
           |    set -e
           |  fi
           |}
           |trap ViashPerformChown EXIT
           |""".stripMargin
      } else {
        ""
      }

    BashWrapperMods(
      preRun = preRun
    )
  }

  private def addComputationalRequirements(
    functionality: Functionality
  ) = {
    // add requirements to parameters
    val extraArgs = 
      """# helper function for filling in extra docker args
        |VIASH_EXTRA_DOCKER_ARGS=""
        |if [ ! -z "$VIASH_META_MEMORY_MB" ]; then
        |  VIASH_EXTRA_DOCKER_ARGS="$VIASH_EXTRA_DOCKER_ARGS --memory=${VIASH_META_MEMORY_MB}m"
        |fi
        |if [ ! -z "$VIASH_META_CPUS" ]; then
        |  VIASH_EXTRA_DOCKER_ARGS="$VIASH_EXTRA_DOCKER_ARGS --cpus=${VIASH_META_CPUS}"
        |fi""".stripMargin

    // return output
    BashWrapperMods(
      extraParams = " $VIASH_EXTRA_DOCKER_ARGS",
      preRun = "\n" + extraArgs
    )
  }
}
