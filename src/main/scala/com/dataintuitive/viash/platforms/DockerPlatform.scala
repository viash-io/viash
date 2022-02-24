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
import com.dataintuitive.viash.helpers.Circe._

case class DockerPlatform(
  id: String = "docker",
  image: String,
  organization: Option[String],
  registry: Option[String] = None,
  tag: Option[Version] = None,
  target_image: Option[String] = None,
  target_organization: Option[String] = None,
  target_registry: Option[String] = None,
  target_tag: Option[Version] = None,
  namespace_separator: String = "_",
  resolve_volume: DockerResolveVolume = Automatic,
  chown: Boolean = true,
  port: OneOrMore[String] = Nil,
  workdir: Option[String] = None,
  setup_strategy: DockerSetupStrategy = IfNeedBePullElseCachedBuild,
  privileged: Boolean = false,
  run_args: OneOrMore[String] = Nil,
  oType: String = "docker",

  // setup variables
  setup: List[Requirements] = Nil,
  apk: Option[ApkRequirements] = None,
  apt: Option[AptRequirements] = None,
  yum: Option[YumRequirements] = None,
  r: Option[RRequirements] = None,
  python: Option[PythonRequirements] = None,
  docker: Option[DockerRequirements] = None,

  // deprecated
  version: Option[Version] = None
) extends Platform {
  override val hasSetup = true

  assert(version.isEmpty, "docker platform: attribute 'version' is deprecated")

  override val requirements: List[Requirements] = {
    val x =
      setup :::
        apk.toList :::
        apt.toList :::
        yum.toList :::
        r.toList :::
        python.toList :::
        docker.toList
    // workaround for making sure that every docker platform creates a new container
    if (x.isEmpty) {
      List(DockerRequirements(
        run = List(":")
      ))
    } else {
      x
    }
  }


  def modifyFunctionality(functionality: Functionality): Functionality = {
    // collect docker args
    val dockerArgs = "-i --rm" +
      port.map(" -p " + _).mkString +
      run_args.map(" " + _).mkString +
      { if (privileged) " --privileged" else "" }

    // create setup
    val (effectiveID, setupMods) = processDockerSetup(functionality)

    // generate automount code
    val dmVol = processDockerVolumes(functionality)

    // generate installationcheck code
    val dmDockerCheck = addDockerCheck()

    // add ---debug flag
    val debuggor = s"""docker run --entrypoint=bash $dockerArgs -v "$$(pwd)":/pwd --workdir /pwd -t '$effectiveID'"""
    val dmDebug = addDockerDebug(debuggor)

    // add ---chown flag
    val dmChown = addDockerChown(functionality, dockerArgs, dmVol.extraParams, effectiveID)

    // compile modifications
    val dm = dmDockerCheck ++ setupMods ++ dmVol ++ dmDebug ++ dmChown

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
        mods = dm
      ))
    )

    fun2.copy(
      resources = bashScript :: fun2.resources.tail
    )
  }

  private def processDockerSetup(functionality: Functionality) = {
    // get dependencies
    val runCommands = requirements.flatMap(_.dockerCommands)

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
          s"""  # create temporary directory to store dockerfile & optional resources in
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
             |  ViashNotice "Building container '$$1' with Dockerfile"
             |  ViashInfo "Running 'docker build -t $$@$buildArgs $$tmpdir'"
             |  save=$$-; set +e
             |  if [ $$${BashWrapper.var_verbosity} -ge $$VIASH_LOGCODE_INFO ]; then
             |    docker build -t $$@$buildArgs $$tmpdir
             |  else
             |    docker build -t $$@$buildArgs $$tmpdir &> $$tmpdir/docker_build.log
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

  private val extraMountsVar = "VIASH_EXTRA_MOUNTS"

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

    val preRun = postParseVolumes + "\n\n" +
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
      preRun = preRun,
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
    val args = functionality.argumentsAndDummies

    def chownCommand(value: String): String = {
      s"""eval docker run --entrypoint=chown $dockerArgs$volExtraParams $fullImageID "$$(id -u):$$(id -g)" --silent --recursive $value"""
    }

    val preRun =
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
           |# change file ownership
           |function ViashPerformChown {
           |  $chownParStr
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
}
