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

package com.dataintuitive.viash.platforms

import com.dataintuitive.viash.functionality._
import com.dataintuitive.viash.functionality.dataobjects._
import com.dataintuitive.viash.functionality.resources._
import com.dataintuitive.viash.platforms.requirements._
import com.dataintuitive.viash.helpers.{Bash, Docker}
import com.dataintuitive.viash.config.Version
import com.dataintuitive.viash.wrapper.{BashWrapper, BashWrapperMods}
import com.dataintuitive.viash.platforms.docker._

case class DockerPlatform(
  id: String = "docker",
  image: String,
  registry: Option[String] = None,
  tag: Option[Version] = None,
  target_image: Option[String] = None,
  target_registry: Option[String] = None,
  target_tag: Option[Version] = None,
  namespace_separator: String = "_",
  resolve_volume: DockerResolveVolume = Automatic,
  chown: Boolean = true,
  port: Option[List[String]] = None,
  workdir: Option[String] = None,
  setup_strategy: DockerSetupStrategy = AlwaysCachedBuild,
  push_strategy: DockerPushStrategy = PushIfNotPresent,

  // setup variables
  setup: List[Requirements] = Nil,
  apk: Option[ApkRequirements] = None,
  apt: Option[AptRequirements] = None,
  r: Option[RRequirements] = None,
  python: Option[PythonRequirements] = None,
  docker: Option[DockerRequirements] = None,

  // deprecated
  version: Option[Version] = None
) extends Platform {
  val `type` = "docker"

  assert(version.isEmpty, "docker platform: attribute 'version' is deprecated")

  val requirements: List[Requirements] =
    setup :::
      apk.toList :::
      apt.toList :::
      r.toList :::
      python.toList :::
      docker.toList

  def modifyFunctionality(functionality: Functionality): Functionality = {
    // collect docker args
    val dockerArgs = "-i --rm" + {
      port.getOrElse(Nil).map(" -p " + _).mkString("")
    }

    // create setup
    val (effectiveID, setupCommands, setupMods) = processDockerSetup(functionality)

    // generate automount code
    val dmVol = processDockerVolumes(functionality)

    // add ---debug flag
    val debuggor = s"""docker run --entrypoint=bash $dockerArgs -v "$$(pwd)":/pwd --workdir /pwd -t $effectiveID"""
    val dmDebug = addDockerDebug(debuggor)

    // add ---chown flag
    val dmChown = addDockerChown(functionality, dockerArgs, dmVol.extraParams, effectiveID)

    // compile modifications
    val dm = setupMods ++ dmVol ++ dmDebug ++ dmChown

    // make commands
    val entrypointStr = functionality.mainScript.get match {
      case _: Executable => "--entrypoint='' "
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
        setupCommands = setupCommands,
        mods = dm
      ))
    )

    fun2.copy(
      resources = Some(bashScript :: fun2.resources.getOrElse(Nil).tail)
    )
  }

  private def processDockerSetup(functionality: Functionality) = {
    // get dependencies
    val runCommands = requirements.flatMap(_.dockerCommands)

    // don't draw defaults from functionality for the from image
    val fromImageInfo = Docker.getImageInfo(
      name = Some(image),
      registry = registry,
      tag = tag.map(_.toString),
      namespaceSeparator = namespace_separator
    )
    val targetImageInfo = Docker.getImageInfo(
      functionality = Some(functionality),
      registry = target_registry,
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

    // if no extra dependencies are needed, the provided image can just be used,
    // otherwise need to construct a separate docker container
    val (viashDockerFile, viashDockerBuild) =
      if (runCommands.isEmpty) {
        ("  :", "  ViashDockerPull $1")
      } else {
        val dockerFile =
          s"FROM ${fromImageInfo.toString}\n\n" +
            runCommands.mkString("\n")

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
          s"""
             |  # create temporary directory to store dockerfile & optional resources in
             |  tmpdir=$$(mktemp -d "$$VIASH_TEMP/viashsetupdocker-${functionality.name}-XXXXXX")
             |  function clean_up {
             |    rm -rf "$$tmpdir"
             |  }
             |  trap clean_up EXIT
             |
             |  # store dockerfile and resources
             |  ViashDockerfile > $$tmpdir/Dockerfile
             |  cp -r $$${BashWrapper.var_resources_dir}/* $$tmpdir
             |
             |  # Build the container
             |  echo "> docker build -t $$@$buildArgs $$tmpdir"
             |  set +e
             |  docker build -t $$@$buildArgs $$tmpdir &> $$tmpdir/docker_build.log
             |  out=$$?
             |  set -e
             |  if [ ! $$out -eq 0 ]; then
             |    echo "> ERROR: Something went wrong building the container $$@"
             |    echo "> Error transcript follows:"
             |    cat $$tmpdir/docker_build.log
             |    echo "> --- end of error transcript"
             |  fi
             |  exit $$out""".stripMargin

        (vdf, vdb)
      }

    val setupCommands = {
      s"""# ViashDockerFile: print the dockerfile to stdout
         |# return : dockerfile required to run this component
         |# examples:
         |#   ViashDockerFile
         |function ViashDockerfile {
         |$viashDockerFile
         |}
         |# ViashDockerBuild: ...
         |function ViashDockerBuild {
         |$viashDockerBuild
         |}
         |
         |# ViashSetup: ...
         |function ViashSetup {
         |  ViashDockerSetup $effectiveID $$$dockerSetupStrategyVar
         |}
         |
         |# ViashPush: ...
         |function ViashPush {
         |  ViashDockerPush $effectiveID $$$dockerPushStrategyVar
         |}""".stripMargin
    }

    val preParse =
      s"""
         |${Bash.ViashDockerFuns}
         |# initialise variables
         |$dockerSetupStrategyVar='${setup_strategy.id}'
         |$dockerPushStrategyVar='${push_strategy.id}'""".stripMargin

    val parsers =
      s"""
         |        ---dss|---docker_setup_strategy)
         |            ${BashWrapper.var_exec_mode}="setup"
         |            $dockerSetupStrategyVar="$$2"
         |            shift 2
         |            ;;
         |        ---docker_setup_strategy=*)
         |            ${BashWrapper.var_exec_mode}="setup"
         |            $dockerSetupStrategyVar=$$(ViashRemoveFlags "$$2")
         |            shift 1
         |            ;;
         |        ---dps|---docker_push_strategy)
         |            ${BashWrapper.var_exec_mode}="push"
         |            $dockerPushStrategyVar="$$2"
         |            shift 2
         |            ;;
         |        ---docker_push_strategy=*)
         |            ${BashWrapper.var_exec_mode}="push"
         |            $dockerPushStrategyVar=$$(ViashRemoveFlags "$$2")
         |            shift 1
         |            ;;
         |        ---dockerfile)
         |            ViashDockerfile
         |            exit 0
         |            ;;""".stripMargin

    val mods = BashWrapperMods(
      preParse = preParse,
      parsers = parsers
    )

    (effectiveID, setupCommands, mods)
  }

  private val extraMountsVar = "VIASH_EXTRA_MOUNTS"
  private val dockerSetupStrategyVar = "VIASH_DOCKER_SETUP_STRATEGY"
  private val dockerPushStrategyVar = "VIASH_DOCKER_PUSH_STRATEGY"

  private def processDockerVolumes(functionality: Functionality) = {
    val args = functionality.argumentsAndDummies

    val preParse =
      s"""
         |${Bash.ViashAbsolutePath}
         |${Bash.ViashAutodetectMount}
         |${Bash.ViashExtractFlags}
         |# initialise variables
         |$extraMountsVar=''""".stripMargin


    val parsers =
      s"""
         |        ---v|---volume)
         |            ${Bash.save(extraMountsVar, Seq("-v \"$2\""))}
         |            shift 2
         |            ;;
         |        ---volume=*)
         |            ${Bash.save(extraMountsVar, Seq("-v $(ViashRemoveFlags \"$2\")"))}
         |            shift 1
         |            ;;""".stripMargin

    val extraParams = s" $$$extraMountsVar"

    val postParseVolumes =
      if (resolve_volume == Automatic) {
        "\n\n# detect volumes from file arguments" +
          args.flatMap {
            case arg: FileObject if arg.multiple =>
              // resolve arguments with multiplicity different from singular args
              val viash_temp = "VIASH_TEST_" + arg.plainName.toUpperCase()
              Some(
                s"""
                   |if [ ! -z "$$${arg.VIASH_PAR}" ]; then
                   |  IFS="${arg.multiple_sep}"
                   |  for var in $$${arg.VIASH_PAR}; do
                   |    unset IFS
                   |    $extraMountsVar="$$$extraMountsVar $$(ViashAutodetectMountArg "$$var")"
                   |    ${BashWrapper.store(viash_temp, "\"$(ViashAutodetectMount \"$var\")\"", Some(arg.multiple_sep)).mkString("\n    ")}
                   |  done
                   |  ${arg.VIASH_PAR}="$$$viash_temp"
                   |fi""".stripMargin)
            case arg: FileObject =>
              Some(
                s"""
                   |if [ ! -z "$$${arg.VIASH_PAR}" ]; then
                   |  $extraMountsVar="$$$extraMountsVar $$(ViashAutodetectMountArg "$$${arg.VIASH_PAR}")"
                   |  ${arg.VIASH_PAR}=$$(ViashAutodetectMount "$$${arg.VIASH_PAR}")
                   |fi""".stripMargin)
            case _ => None
          }.mkString("")
      } else {
        ""
      }

    val postParse = postParseVolumes + "\n\n" +
      s"""# Always mount the resource directory
         |$extraMountsVar="$$$extraMountsVar $$(ViashAutodetectMountArg "$$${BashWrapper.var_resources_dir}")"
         |${BashWrapper.var_resources_dir}=$$(ViashAutodetectMount "$$${BashWrapper.var_resources_dir}")
         |
         |# Always mount the VIASH_TEMP directory
         |$extraMountsVar="$$$extraMountsVar $$(ViashAutodetectMountArg "$$VIASH_TEMP")"
         |VIASH_TEMP=$$(ViashAutodetectMount "$$VIASH_TEMP")""".stripMargin

    BashWrapperMods(
      preParse = preParse,
      parsers = parsers,
      postParse = postParse,
      extraParams = extraParams
    )
  }

  private def addDockerDebug(debugCommand: String) = {
    val parsers =
      s"""
         |        ---debug)
         |            echo "+ $debugCommand"
         |            $debugCommand
         |            exit 0
         |            ;;""".stripMargin


    BashWrapperMods(
      parsers = parsers
    )
  }

  private def addDockerChown(
    functionality: Functionality,
    dockerArgs: String,
    volExtraParams: String,
    fullImageID: String,
  ) = {
    val args = functionality.argumentsAndDummies

    def chownCommand(value: String): String = {
      s"""eval docker run --entrypoint=chown $dockerArgs$volExtraParams $fullImageID "$$(id -u):$$(id -g)" -R $value"""
    }

    val postParse =
      if (chown) {
        // chown output files/folders
        val chownPars = args
          .filter(a => a.isInstanceOf[FileObject] && a.direction == Output)
          .map(arg => {

            // resolve arguments with multiplicity different from
            // singular args
            if (arg.multiple) {
              val viash_temp = "VIASH_TEST_" + arg.plainName.toUpperCase()
              s"""
                 |if [ ! -z "$$${arg.VIASH_PAR}" ]; then
                 |  IFS="${arg.multiple_sep}"
                 |  for var in $$${arg.VIASH_PAR}; do
                 |    unset IFS
                 |    ${chownCommand("\"$var\"")}
                 |  done
                 |  ${arg.VIASH_PAR}="$$$viash_temp"
                 |fi""".stripMargin
            } else {
              s"""
                 |if [ ! -z "$$${arg.VIASH_PAR}" ]; then
                 |  ${chownCommand("\"$" + arg.VIASH_PAR + "\"")}
                 |fi""".stripMargin
            }
          })

        val chownParStr =
          if (chownPars.isEmpty) ":"
          else chownPars.mkString("").split("\n").mkString("\n  ")

        s"""
           |
           |# change file ownership
           |function viash_perform_chown {
           |  $chownParStr
           |}
           |trap viash_perform_chown EXIT
           |""".stripMargin
      } else {
        ""
      }

    BashWrapperMods(
      postParse = postParse
    )
  }
}
