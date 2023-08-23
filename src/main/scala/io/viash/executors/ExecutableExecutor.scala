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

package io.viash.executors



import io.viash.config.Config
import io.viash.config.{Info => ConfigInfo}

import io.viash.functionality.Functionality

// todo: remove
import io.viash.platforms.docker._
import io.viash.functionality.resources.Executable
import io.viash.functionality.arguments.FileArgument
import io.viash.functionality.arguments.Output
import io.viash.functionality.resources.BashScript

import io.viash.engines._

import io.viash.wrapper.BashWrapper
import io.viash.wrapper.BashWrapperMods

import io.viash.helpers.Bash

import io.viash.helpers.data_structures._

import io.viash.schemas._
import io.viash.engines.DockerEngine
import io.viash.engines.NativeEngine
final case class ExecutableExecutor(
  @description("Name of the executor. As with all executors, you can give an executor a different name. By specifying `id: foo`, you can target this executor (only) by specifying `...` in any of the Viash commands.")
  @example("id: foo", "yaml")
  @default("executable")
  id: String = "executable",

  @description("A list of enabled ports. This doesn't change the Dockerfile but gets added as a command-line argument at runtime.")
  @example(
    """port:
      |  - 80
      |  - 8080
      |""".stripMargin,
      "yaml")
  @default("Empty")
  port: OneOrMore[String] = Nil,

  @description("The working directory when starting the engine. This doesn't change the Dockerfile but gets added as a command-line argument at runtime.")
  @example("workdir: /home/user", "yaml")
  workdir: Option[String] = None,

  @description(
    """The Docker setup strategy to use when building a docker engine enrivonment.
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
  docker_setup_strategy: DockerSetupStrategy = IfNeedBePullElseCachedBuild,

  @description("Provide runtime arguments to Docker. See the documentation on [`docker run`](https://docs.docker.com/engine/reference/run/) for more information.")
  @default("Empty")
  docker_run_args: OneOrMore[String] = Nil,

) extends Executor {
  val `type` = "executable"

  def generateExecutor(config: Config, testing: Boolean): ExecutorResources = {
    val engines = config.getEngines

    /*
     * Construct bashwrappermods
     */
    val profileMods = engines.map{
      case d: DockerEngine => 
        dockerConfigMods(d, config, testing)
      case n: NativeEngine =>
        nativeConfigMods()
      case _ =>
        BashWrapperMods()
    }
    

    val profilesDm = generateProfiles(config.functionality, engines)

    // create new bash script
    val mainScript = Some(BashScript(
      dest = Some(config.functionality.name),
      text = Some(BashWrapper.wrapScript(
        executor = "eval $VIASH_CMD",
        functionality = config.functionality,
        mods = profileMods.foldLeft(profilesDm)(_ ++ _),
        config = config
      ))
    ))

    // return output
    ExecutorResources(
      mainScript = mainScript,
      additionalResources = config.functionality.additionalResources
    )
  }

  // todo: support multiple containers
  private def generateProfiles(functionality: Functionality, containers: List[Engine]): BashWrapperMods = {

    // todo: allow setting the default profile
    val preParse = 
      s"""
        |# initialise variables
        |VIASH_PROFILE="${containers.head.id}"""".stripMargin

    BashWrapperMods(
      preParse = preParse
    )
  }

  // todo: support multiple containers
  private def nativeConfigMods(): BashWrapperMods = {
    val preRun =
      s"""
        |if [ "$$VIASH_PROFILE" == "native" ]; then
        |  VIASH_CMD="bash"
        |fi""".stripMargin
      
    BashWrapperMods(
      preRun = preRun
    )
  }

  private def dockerConfigMods(dockerEngine: DockerEngine, config: Config, testing: Boolean): BashWrapperMods = {
    val effectiveID = dockerEngine.getTargetIdentifier(config.functionality)

    // generate docker container setup code
    val dmSetup = dockerGenerateSetup(config.functionality, config.info, testing, dockerEngine, effectiveID)

    // generate automount code
    val dmVol = dockerDetectMounts(config.functionality)

    // generate installationcheck code
    val dmDockerCheck = dockerAddInstallationCheck()

    // add ---debug flag
    val debuggor = s"""docker run --entrypoint=bash $dockerArgs -v "$$(pwd)":/pwd --workdir /pwd -t '$effectiveID'"""
    val dmDebug = addDebug(debuggor)

    // add ---chown flag
    val dmChown = dockerAddChown(config.functionality, dockerArgs, dmVol.extraParams, effectiveID)

    // process cpus and memory_b
    val dmReqs = dockerAddComputationalRequirements(config.functionality)

    val dmCmd = dockerGenerateCommand(config.functionality, effectiveID)

    // compile bashwrappermods for Docker
    dmDockerCheck ++ dmSetup ++ dmVol ++ dmDebug ++ dmChown ++ dmReqs ++ dmCmd
  }

  private def dockerGenerateCommand(functionality: Functionality, dockerContainer: String): BashWrapperMods = {

    // collect runtime docker arguments
    val dockerEntrypoint = functionality.mainScript match {
      case Some(_: Executable) => " --entrypoint=''"
      case _ => " --entrypoint=bash"
    }

    val workdirStr = workdir.map(" --workdir '" + _ + "'").getOrElse("")

    // todo: allow setting the default profile
    val preParse = 
      s"""
        |# initialise variables
        |VIASH_DOCKER_RUN_ARGS=($dockerArgs$workdirStr)""".stripMargin

    val preRun =
      s"""
        |if [ "$$VIASH_PROFILE" == "docker" ]; then
        |  VIASH_CMD="docker run$dockerEntrypoint $${VIASH_DOCKER_RUN_ARGS[@]} $${VIASH_UNIQUE_MOUNTS[@]} $dockerContainer
        |fi""".stripMargin
      
    BashWrapperMods(
      preParse = preParse,
      preRun = preRun
    )
  }

  private def dockerArgs: String = {
    "-i --rm" +
      port.map(" -p " + _).mkString +
      docker_run_args.map(" " + _).mkString
  }



  /*
   * DOCKER MODS
   */
  private def dockerGenerateSetup(
    functionality: Functionality,
    info: Option[ConfigInfo],
    testing: Boolean,
    dockerImage: DockerEngine,
    effectiveID: String
  ): BashWrapperMods = {
    
    // get list of all the commands that should be available in the container
    val commandsToCheck = functionality.requirements.commands ::: List("bash")
    val commandsToCheckStr = commandsToCheck.mkString("'", "' '", "'")
    
    val preParse =
      s"""${Bash.ViashDockerFuns}
        |
        |# ViashDockerFile: print the dockerfile to stdout
        |# return : dockerfile required to run this component
        |# examples:
        |#   ViashDockerFile
        |function ViashDockerfile {
        |  cat << 'VIASHDOCKER'
        |${dockerImage.dockerFile(functionality, info, testing)}
        |VIASHDOCKER
        |}
        |
        |# ViashDockerBuild: build a docker container
        |# $$1              : image identifier with format `[registry/]image[:tag]`
        |# exit code $$?    : whether or not the image was built
        |function ViashDockerBuild {
        |${dockerImage.buildDockerContainerInBash(functionality, testing)}
        |  ViashDockerCheckCommands "$$1" $commandsToCheckStr
        |}""".stripMargin

    val parsers =
      s"""
        |        ---setup)
        |            VIASH_MODE='setup'
        |            VIASH_SETUP_STRATEGY="$$2"
        |            shift 1
        |            ;;
        |        ---setup=*)
        |            VIASH_MODE='setup'
        |            VIASH_SETUP_STRATEGY="$$(ViashRemoveFlags "$$1")"
        |            shift 2
        |            ;;
        |        ---dockerfile)
        |            ViashDockerfile
        |            exit 0
        |            ;;""".stripMargin

    val postParse =
      s"""
         |if [ $$VIASH_MODE == "setup" ]; then
         |  if [ "$$VIASH_PROFILE" == "docker" ]; then
         |    ViashDockerSetup '$effectiveID' "$$VIASH_DOCKER_SETUP_STRATEGY"
         |    exit 0
         |  else
         |    ViashError "Profile '$$VIASH_PROFILE' does not support setup mode."
         |    exit 1
         |  fi
         |fi
         |if [ "$$VIASH_PROFILE" == "docker" ]; then
         |  ViashDockerSetup '$effectiveID' ${IfNeedBePullElseCachedBuild.id}
         |fi""".stripMargin

    BashWrapperMods(
      preParse = preParse,
      parsers = parsers,
      postParse = postParse
    )
  }

  

  private def dockerDetectMounts(functionality: Functionality): BashWrapperMods = {
    val args = functionality.getArgumentLikes(includeMeta = true)
    
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
            |    VIASH_DIRECTORY_MOUNTS+=( "$$(ViashDockerAutodetectMountArg "$$var")" )
            |    var=$$(ViashDockerAutodetectMount "$$var")
            |    $viash_temp+=( "$$var" )$chownIfOutput
            |  done
            |  ${arg.VIASH_PAR}=$$(IFS='${Bash.escapeString(arg.multiple_sep, quote = true)}' ; echo "$${$viash_temp[*]}")
            |fi""".stripMargin
        )
      case arg: FileArgument =>
        val chownIfOutput = if (arg.direction == Output) "\n  VIASH_CHOWN_VARS+=( \"$" + arg.VIASH_PAR + "\" )" else ""
        Some(
          s"""
            |if [ ! -z "$$${arg.VIASH_PAR}" ]; then
            |  VIASH_DIRECTORY_MOUNTS+=( "$$(ViashDockerAutodetectMountArg "$$${arg.VIASH_PAR}")" )
            |  ${arg.VIASH_PAR}=$$(ViashDockerAutodetectMount "$$${arg.VIASH_PAR}")$chownIfOutput
            |fi""".stripMargin
        )
      case _ => None
    }

    // if there are no mounts, return empty mods
    if (detectMounts.isEmpty) {
      return BashWrapperMods()
    }

    val preParse =
      s"""
         |${Bash.ViashAbsolutePath}
         |${Bash.ViashDockerAutodetectMount}
         |# initialise variables
         |VIASH_DIRECTORY_MOUNTS=()""".stripMargin
    
    val preRun =
      f"""
        |if [ "$$VIASH_PROFILE" == "docker" ]; then
        |  # detect volumes from file arguments
        |  VIASH_CHOWN_VARS=()${detectMounts.mkString("")}
        |  
        |  # get unique mounts
        |  VIASH_UNIQUE_MOUNTS=($$(for val in "$${VIASH_DIRECTORY_MOUNTS[@]}"; do echo "$$val"; done | sort -u))
        |fi
        |""".stripMargin
    
  
    val stripAutomounts = args.flatMap {
      case arg: FileArgument if arg.multiple =>
        // resolve arguments with multiplicity different from singular args
        val viash_temp = "VIASH_TEST_" + arg.plainName.toUpperCase()
        Some(
          s"""
            |  if [ ! -z "$$${arg.VIASH_PAR}" ]; then
            |    unset $viash_temp
            |    IFS='${Bash.escapeString(arg.multiple_sep, quote = true)}'
            |    for var in $$${arg.VIASH_PAR}; do
            |      unset IFS
            |      ${BashWrapper.store("ViashDockerStripAutomount", viash_temp, "\"$(ViashDockerStripAutomount \"$var\")\"", Some(arg.multiple_sep)).mkString("\n    ")}
            |    done
            |    ${arg.VIASH_PAR}="$$$viash_temp"
            |  fi""".stripMargin
        )
      case arg: FileArgument =>
        Some(
          s"""
            |  if [ ! -z "$$${arg.VIASH_PAR}" ]; then
            |    ${arg.VIASH_PAR}=$$(ViashDockerStripAutomount "$$${arg.VIASH_PAR}")
            |  fi""".stripMargin
        )
      case _ => None
    }
    
    val postRun =
      s"""
        |if [ "$$VIASH_PROFILE" == "docker" ]; then
        |  # strip viash automount from file paths
        |  ${stripAutomounts.mkString("")}
        |fi""".stripMargin

    BashWrapperMods(
      preParse = preParse,
      preRun = preRun,
      postRun = postRun
    )
  }


  private def dockerAddInstallationCheck() = {
    val postParse =
      s"""
         |ViashDockerInstallationCheck""".stripMargin

    BashWrapperMods(
      postParse = postParse
    )
  }

  private def addDebug(dockerDebugCommand: String) = {
    val parsers =
      s"""
         |        ---debug)
         |            VIASH_MODE='debug'
         |            shift 1
         |            ;;""".stripMargin

    val postParse =
      s"""
         |if [ $$VIASH_MODE == "debug" ]; 
         |  if [ $$VIASH_PROFILE == "docker" ]; then
         |    ViashNotice "+ $dockerDebugCommand"
         |    $dockerDebugCommand
         |    exit 0
         |  else
         |    ViashError "Profile '$$VIASH_PROFILE' does not support debug mode."
         |    exit 1
         |  fi
         |fi""".stripMargin

    BashWrapperMods(
      parsers = parsers,
      postParse = postParse
    )
  }

  private def dockerAddChown(
    functionality: Functionality,
    dockerArgs: String,
    volExtraParams: String,
    fullImageID: String,
  ) = {
    val preRun =
      s"""
        |if [ "$$VIASH_PROFILE" == "docker" ]; then
        |  # change file ownership
        |  function ViashPerformChown {
        |    if (( $${#VIASH_CHOWN_VARS[@]} )); then
        |      set +e
        |      eval docker run --entrypoint=chown $dockerArgs$volExtraParams $fullImageID "$$(id -u):$$(id -g)" --silent --recursive $${VIASH_CHOWN_VARS[@]}
        |      set -e
        |    fi
        |  }
        |  trap ViashPerformChown EXIT
        |fi""".stripMargin

    BashWrapperMods(
      preRun = preRun
    )
  }

  private def dockerAddComputationalRequirements(
    functionality: Functionality
  ): BashWrapperMods = {
    // add requirements to parameters
    val preRun = 
      """
        |if [ "$$VIASH_PROFILE" == "docker" ]; then
        |  # helper function for filling in extra docker args
        |  if [ ! -z "$VIASH_META_MEMORY_MB" ]; then
        |    VIASH_DOCKER_RUN_ARGS+=("--memory=${VIASH_META_MEMORY_MB}m")
        |  fi
        |  if [ ! -z "$VIASH_META_CPUS" ]; then
        |    VIASH_DOCKER_RUN_ARGS+=("--cpus=${VIASH_META_CPUS}")
        |  fi
        |fi""".stripMargin

    // return output
    BashWrapperMods(
      preRun = preRun
    )
  }
}
