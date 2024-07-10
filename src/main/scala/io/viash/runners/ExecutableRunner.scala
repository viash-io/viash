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

package io.viash.runners

import io.viash.config.Config
import io.viash.config.BuildInfo

// todo: remove
import io.viash.config.resources.Executable
import io.viash.config.resources.BashScript
import io.viash.config.arguments.{FileArgument, Input, Output}

import io.viash.engines._
import io.viash.engines.requirements.DockerRequirements
import io.viash.runners.executable._

import io.viash.wrapper.BashWrapper
import io.viash.wrapper.BashWrapperMods

import io.viash.helpers.Bash
import io.viash.helpers.data_structures._

import io.viash.schemas._

@description(
  """Run code as an executable.
    |
    |This runner is the default runner. It will generate a bash script that can be run directly.
    |
    |This runner is also used for the @[native](native_engine) engine.
    |
    |This runner is also used for the @[docker](docker_engine) engine.
    |""".stripMargin)
@example(
  """runners:
    |  - type: executable
    |    port: 8080
    |""".stripMargin,
  "yaml")
@subclass("executable")
final case class ExecutableRunner(
  @description("Name of the runner. As with all runners, you can give an runner a different name. By specifying `id: foo`, you can target this executor (only) by specifying `...` in any of the Viash commands.")
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

  `type`: String = "executable"
) extends Runner {

  def generateRunner(config: Config, testing: Boolean): RunnerResources = {
    val engines = config.engines
    
    /*
     * Construct bashwrappermods
     */
    val mods =
      generateEngineVariable(config) ++
      nativeConfigMods(config) ++
      dockerConfigMods(config, testing)

    // create new bash script
    val mainScript = Some(BashScript(
      dest = Some(config.name),
      text = Some(BashWrapper.wrapScript(
        executor = "eval $VIASH_CMD",
        mods = mods,
        config = config
      ))
    ))

    // return output
    RunnerResources(
      mainScript = mainScript,
      additionalResources = config.additionalResources
    )
  }

  private def oneOfEngines(engines: List[Engine]): String = {
    engines
      .map(engine => s""" [ "$$VIASH_ENGINE_ID" == "${engine.id}" ] """)
      .mkString(" || ")
  }

  private def noneOfEngines(engines: List[Engine]): String = {
    engines
      .map(engine => s"""" [ $$VIASH_ENGINE_ID" != "${engine.id}" ] """)
      .mkString(" && ")
  }

  private def generateEngineVariable(config: Config): BashWrapperMods = {
    val engines = config.engines

    // TODO: allow setting the default engine
    val preParse = 
      s"""
        |# initialise variables
        |VIASH_MODE='run'
        |VIASH_ENGINE_ID='${engines.head.id}'""".stripMargin

    val parsers =
      s"""
        |        ---engine)
        |            VIASH_ENGINE_ID="$$2"
        |            shift 2
        |            ;;
        |        ---engine=*)
        |            VIASH_ENGINE_ID="$$(ViashRemoveFlags "$$1")"
        |            shift 1
        |            ;;""".stripMargin

    val typeSetterStrs = engines.groupBy(_.`type`).map{ case (engineType, engineList) => 
      s""" ${oneOfEngines(engineList)} ; then
        |  VIASH_ENGINE_TYPE='${engineType}'""".stripMargin
    }
    val postParse =
      s"""
        |if ${typeSetterStrs.mkString("\nelif ")}
        |else
        |  ViashError "Engine '$$VIASH_ENGINE_ID' is not recognized. Options are: ${engines.map(_.id).mkString(", ")}."
        |  exit 1
        |fi""".stripMargin

    BashWrapperMods(
      preParse = preParse,
      parsers = parsers,
      postParse = postParse
    )
  }

  private def nativeConfigMods(config: Config): BashWrapperMods = {
    val engines = config.engines.flatMap{
      case e: NativeEngine => Some(e)
      case _ => None
    }

    if (engines.isEmpty) {
      return BashWrapperMods()
    }

    // eval already present, so an executable runs with `eval x` while scripts run with `eval bash x`
    val cmd = config.mainScript match {
      case Some(_: Executable) => ""
      case _ => "bash"
    }

    val preRun =
      s"""
        |if ${oneOfEngines(engines)} ; then
        |  if [ "$$VIASH_MODE" == "run" ]; then
        |    VIASH_CMD="$cmd"
        |  else
        |    ViashError "Engine '$$VIASH_ENGINE_ID' does not support mode '$$VIASH_MODE'."
        |    exit 1
        |  fi
        |fi""".stripMargin
      
    BashWrapperMods(
      preRun = preRun
    )
  }

  /*
   * DOCKER MODS
   */
  private def dockerConfigMods(config: Config, testing: Boolean): BashWrapperMods = {
    val engines = config.engines.flatMap{
      case e: DockerEngine => Some(e)
      case _ => None
    }

    if (engines.isEmpty) {
      return BashWrapperMods()
    }
    
    // generate docker container setup code
    val dmSetup = dockerGenerateSetup(config, config.build_info, testing, engines)

    // generate automount code
    val dmVol = dockerDetectMounts(config)

    // add ---chown flag
    val dmChown = dockerAddChown(config)

    // process cpus and memory_b
    val dmReqs = dockerAddComputationalRequirements()

    // generate docker command
    val dmCmd = dockerGenerateCommand(config)

    // compile bashwrappermods for Docker
    dmSetup ++ dmVol ++ dmChown ++ dmReqs ++ dmCmd
  }

  private def dockerGenerateSetup(
    config: Config,
    info: Option[BuildInfo],
    testing: Boolean,
    engines: List[DockerEngine]
  ): BashWrapperMods = {
    
    // get list of all the commands that should be available in the container
    val commandsToCheck = config.requirements.commands ::: List("bash")
    val commandsToCheckStr = commandsToCheck.mkString("'", "' '", "'")

    val dockerFiles = engines.map { engine =>
      s"""
        |  if [[ "$$engine_id" == "${engine.id}" ]]; then
        |    cat << 'VIASHDOCKER'
        |${engine.dockerFile(config, info, testing)}
        |VIASHDOCKER
        |  fi""".stripMargin
    }

    val dockerBuildArgs = engines.map { engine =>
      val setups = engine.setup ::: { if (testing) engine.test_setup else Nil }
      val dockerRequirements = 
        setups.flatMap{
          case d: DockerRequirements => d.build_args
          case _ => Nil
        }
      val buildArgs = dockerRequirements.map("--build-arg '" + _ + "'").mkString(" ")

      s"""
        |  if [[ "$$engine_id" == "${engine.id}" ]]; then
        |    echo "${buildArgs}"
        |  fi""".stripMargin
    }
    
    val preParse =
      s"""${Bash.ViashDockerFuns}
        |
        |# ViashDockerFile: print the dockerfile to stdout
        |# $$1    : engine identifier
        |# return : dockerfile required to run this component
        |# examples:
        |#   ViashDockerFile
        |function ViashDockerfile {
        |  local engine_id="$$1"
        |${dockerFiles.mkString}
        |}
        |
        |# ViashDockerBuildArgs: return the arguments to pass to docker build
        |# $$1    : engine identifier
        |# return : arguments to pass to docker build
        |function ViashDockerBuildArgs {
        |  local engine_id="$$1"
        |${dockerBuildArgs.mkString}
        |}""".stripMargin

    val parsers =
      s"""
        |        ---setup)
        |            VIASH_MODE='setup'
        |            VIASH_SETUP_STRATEGY="$$2"
        |            shift 2
        |            ;;
        |        ---setup=*)
        |            VIASH_MODE='setup'
        |            VIASH_SETUP_STRATEGY="$$(ViashRemoveFlags "$$1")"
        |            shift 1
        |            ;;
        |        ---dockerfile)
        |            VIASH_MODE='dockerfile'
        |            shift 1
        |            ;;
        |        ---debug)
        |            VIASH_MODE='debug'
        |            shift 1
        |            ;;""".stripMargin

    val setDockerImageId = engines.map { engine => 
      s"""[[ "$$VIASH_ENGINE_ID" == '${engine.id}' ]]; then
        |    VIASH_DOCKER_IMAGE_ID='${engine.getTargetIdentifier(config).toString()}'""".stripMargin  
    }.mkString("if ", "\n  elif ", "\n  fi")

    val postParse =
      s"""
        |if [[ "$$VIASH_ENGINE_TYPE" == "docker" ]]; then
        |  # check if docker is installed properly
        |  ViashDockerInstallationCheck
        |
        |  # determine docker image id
        |  $setDockerImageId
        |
        |  # print dockerfile
        |  if [ "$$VIASH_MODE" == "dockerfile" ]; then
        |    ViashDockerfile "$$VIASH_ENGINE_ID"
        |    exit 0
        |  
        |  # enter docker container
        |  elif [[ "$$VIASH_MODE" == "debug" ]]; then
        |    VIASH_CMD="docker run --entrypoint=bash $${VIASH_DOCKER_RUN_ARGS_ARRAY[@]} -v '$$(pwd)':/pwd --workdir /pwd -t $$VIASH_DOCKER_IMAGE_ID"
        |    ViashNotice "+ $$VIASH_CMD"
        |    eval $$VIASH_CMD
        |    exit 
        |
        |  # build docker image
        |  elif [ "$$VIASH_MODE" == "setup" ]; then
        |    ViashDockerSetup "$$VIASH_DOCKER_IMAGE_ID" "$$VIASH_SETUP_STRATEGY"
        |    ViashDockerCheckCommands "$$VIASH_DOCKER_IMAGE_ID" $commandsToCheckStr
        |    exit 0
        |  fi
        |
        |  # check if docker image exists
        |  ViashDockerSetup "$$VIASH_DOCKER_IMAGE_ID" ${docker_setup_strategy.id}
        |  ViashDockerCheckCommands "$$VIASH_DOCKER_IMAGE_ID" $commandsToCheckStr
        |fi""".stripMargin

          
    BashWrapperMods(
      preParse = preParse,
      parsers = parsers,
      postParse = postParse
    )
  }

  

  private def dockerDetectMounts(config: Config): BashWrapperMods = {
    val args = config.getArgumentLikes(includeMeta = true)
    
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
        |if [[ "$$VIASH_ENGINE_TYPE" == "docker" ]]; then
        |  # detect volumes from file arguments
        |  VIASH_CHOWN_VARS=()${detectMounts.mkString("")}
        |  
        |  # get unique mounts
        |  VIASH_UNIQUE_MOUNTS=($$(for val in "$${VIASH_DIRECTORY_MOUNTS[@]}"; do echo "$$val"; done | sort -u))
        |fi
        |""".stripMargin
    
  
    val stripAutomounts = args.flatMap {
      case arg: FileArgument if arg.multiple && arg.direction == Input =>
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
        |if [[ "$$VIASH_ENGINE_TYPE" == "docker" ]]; then
        |  # strip viash automount from file paths
        |  ${stripAutomounts.mkString("")}
        |fi""".stripMargin

    BashWrapperMods(
      preParse = preParse,
      preRun = preRun,
      postRun = postRun
    )
  }




  private def dockerAddChown(config: Config): BashWrapperMods = {
    // TODO: how are mounts added to this section?
    val preRun =
      s"""
        |if [[ "$$VIASH_ENGINE_TYPE" == "docker" ]]; then
        |  # change file ownership
        |  function ViashPerformChown {
        |    if (( $${#VIASH_CHOWN_VARS[@]} )); then
        |      set +e
        |      VIASH_CMD="docker run --entrypoint=bash --rm $${VIASH_UNIQUE_MOUNTS[@]} $$VIASH_DOCKER_IMAGE_ID -c 'chown $$(id -u):$$(id -g) --silent --recursive $${VIASH_CHOWN_VARS[@]}'"
        |      ViashDebug "+ $$VIASH_CMD"
        |      eval $$VIASH_CMD
        |      set -e
        |    fi
        |  }
        |  trap ViashPerformChown EXIT
        |fi""".stripMargin

    BashWrapperMods(
      preRun = preRun
    )
  }

  private def dockerAddComputationalRequirements(): BashWrapperMods = {
    // add requirements to parameters
    val preRun = 
      s"""
        |if [[ "$$VIASH_ENGINE_TYPE" == "docker" ]]; then
        |  # helper function for filling in extra docker args
        |  if [ ! -z "$$VIASH_META_MEMORY_B" ]; then
        |    VIASH_DOCKER_RUN_ARGS_ARRAY+=("--memory=$${VIASH_META_MEMORY_B}")
        |  fi
        |  if [ ! -z "$$VIASH_META_CPUS" ]; then
        |    VIASH_DOCKER_RUN_ARGS_ARRAY+=("--cpus=$${VIASH_META_CPUS}")
        |  fi
        |fi""".stripMargin

    // return output
    BashWrapperMods(
      preRun = preRun
    )
  }


  private def dockerGenerateCommand(config: Config): BashWrapperMods = {

    // collect runtime docker arguments
    val entrypointStr = config.mainScript match {
      case Some(_: Executable) => " --entrypoint=''"
      case _ => " --entrypoint=bash"
    }

    val workdirStr = workdir.map(" --workdir '" + _ + "'").getOrElse("")

    val dockerArgs =
      "-i --rm" +
        port.map(" -p " + _).mkString +
        docker_run_args.map(" " + _).mkString
    
    val preParse = 
      s"""
        |# initialise docker variables
        |VIASH_DOCKER_RUN_ARGS_ARRAY=($dockerArgs $$VIASH_DOCKER_RUN_ARGS)
        """.stripMargin

    val preRun =
      s"""
        |if [[ "$$VIASH_ENGINE_TYPE" == "docker" ]]; then
        |  VIASH_CMD="docker run$entrypointStr$workdirStr $${VIASH_DOCKER_RUN_ARGS_ARRAY[@]} $${VIASH_UNIQUE_MOUNTS[@]} $$VIASH_DOCKER_IMAGE_ID"
        |fi""".stripMargin
      
    BashWrapperMods(
      preParse = preParse,
      preRun = preRun
    )
  }
}
