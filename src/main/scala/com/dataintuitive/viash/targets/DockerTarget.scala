package com.dataintuitive.viash.targets

import com.dataintuitive.viash.functionality._
import com.dataintuitive.viash.functionality.platforms.{NativePlatform, BashPlatform}
import com.dataintuitive.viash.targets.environments._
import java.nio.file.Paths
import com.dataintuitive.viash.helpers.BashHelper

case class DockerTarget(
  image: String,
  volumes: Option[List[Volume]] = None,
  port: Option[List[String]] = None,
  workdir: Option[String] = None,
  apt: Option[AptEnvironment] = None,
  r: Option[REnvironment] = None,
  python: Option[PythonEnvironment] = None
) extends Target {
  val `type` = "docker"

  def modifyFunctionality(functionality: Functionality) = {
    val resourcesPath = "/app"

    // construct dockerfile, if needed
    val dockerFile = makeDockerFile(functionality, resourcesPath)

    // get image name
    val runImageName = if (dockerFile.isEmpty) image else "viash_autogen/" + functionality.name

    // add extra arguments to the functionality file for each of the volumes
    val volInputs = volumes.getOrElse(Nil).map(vol =>
      StringObject(
        name = "--" + vol.name,
        description = Some(s"Local path to mount directory for volume '${vol.name}'."),
        required = Some(true),
        direction = Input
      )
    )
    val fun2 = functionality.copy(
      arguments = functionality.arguments ::: volInputs
    )

    // get main script
    val mainResource = fun2.mainResource.get
    val mainPath = Paths.get(resourcesPath, mainResource.name).toFile().getPath()

    /**
     * Note: This is not a good place to check for platform types, separation of concern-wise.
     */

    val executionCode = fun2.platform match {
      case None => mainPath
      case Some(NativePlatform) =>
        mainResource.path.map(_ + " $VIASHARGS").getOrElse("echo No command provided")
      case Some(BashPlatform) =>
        s"""
          |set -- $$VIASHARGS
          |${BashHelper.escape(fun2.mainCodeWithArgParse.get)}
          |""".stripMargin
      case Some(pl) => {
        s"""
          |if [ ! -d "$resourcesPath" ]; then mkdir "$resourcesPath"; fi
          |cat > "$mainPath" << 'VIASHMAIN'
          |${BashHelper.escape(fun2.mainCodeWithArgParse.get)}
          |VIASHMAIN
          |${pl.command(mainPath)} $$VIASHARGS
          |""".stripMargin
      }
    }

    // generate bash document
    val (heredocStart, heredocEnd) = fun2.platform match {
      case None | Some(NativePlatform) => ("", "")
      case Some(_) => ("cat << VIASHEOF | ", "\nVIASHEOF")
    }

    val dockerArgs = generateDockerRunArgs(functionality)

    val bash =
      Resource(
        name = functionality.name,
        code = Some(s"""#!/bin/bash
          |
          |${generateBashParsers(fun2, runImageName)}
          |
          |if [ ! "$$(docker images -q $runImageName)" ]; then
          |  echo "The Docker container for this application does not seem to have been built yet."
          |  echo "Try running '${functionality.name} ---setup' first."
          |  exit 1
          |fi
          |
          |${heredocStart}docker run $dockerArgs $runImageName $executionCode$heredocEnd""".stripMargin),
        isExecutable = true
      )

    fun2.copy(
      resources =
        fun2.resources.filterNot(_.name.startsWith("main")) :::
        dockerFile :::
        List(bash)
    )
  }

  def makeDockerFile(functionality: Functionality, resourcesPath: String) = {
    // get dependencies
    val aptInstallCommands = apt.map(_.getInstallCommands()).getOrElse(Nil)
    val rInstallCommands = r.map(_.getInstallCommands()).getOrElse(Nil)
    val pythonInstallCommands = python.map(_.getInstallCommands()).getOrElse(Nil)
    val resourceNames = functionality.resources.map(_.name).filterNot(_.startsWith("main"))

    val deps = List(aptInstallCommands, rInstallCommands, pythonInstallCommands, resourceNames).flatten

    // if no extra dependencies are needed, the provided image can just be used,
    // otherwise need to construct a separate docker container
    if (deps.isEmpty) {
      Nil
    } else {
      List(Resource(
        name = "Dockerfile",
        code = Some(
          s"FROM $image\n" +
            {
              if (!aptInstallCommands.isEmpty) {
                "\n" +
                "# install apt requirements\n" +
                aptInstallCommands.mkString("RUN ", " && \\\n  ", "\n")
              } else {
                ""
              }
            } +
            {
              if (!rInstallCommands.isEmpty) {
                "\n" +
                "# install R requirements\n" +
                rInstallCommands.mkString("RUN ", " && \\\n  ", "\n")
              } else {
                ""
              }
            } +
            {
              if (!pythonInstallCommands.isEmpty) {
                "\n" +
                "# install Python requirements\n" +
                pythonInstallCommands.mkString("RUN ", " && \\\n  ", "\n")
              } else {
                ""
              }
            } +
            {
              if (!resourceNames.isEmpty) {
                s"""
                  |# copy resources
                  |COPY ${resourceNames.mkString(" ")} $resourcesPath/
                  |WORKDIR $resourcesPath
                  """.stripMargin
              } else {
                ""
              }
            }
        )
      ))
    }
  }

  def generateDockerRunArgs(functionality: Functionality) = {
    // process port parameter
    val portStr = port.getOrElse(Nil).map("-p " + _ + " ").mkString("")

    // process volume parameter
    val volumesGet = volumes.getOrElse(Nil)
    val volStr = volumesGet.map(vol => s"-v $$${vol.variable}:${vol.mount} ").mkString("")

    // check whether entrypoint should be set to bash
    val entrypointStr = functionality.platform match {
      case None | Some(NativePlatform) => ""
      case _ => "--entrypoint bash "
    }

    portStr + volStr + entrypointStr + "-i"
  }

  def generateBashParsers(functionality: Functionality, runImageName: String) = {
    // remove extra volume args if extra parameters are not desired
    val storeVariable = functionality.platform match {
      case None | Some(NativePlatform) => None
      case _ => Some("VIASHARGS")
    }

    // generate volume checks
    val volumeDefaults =
      if (volumes.getOrElse(Nil).isEmpty) {
        ""
      } else {
        volumes.getOrElse(Nil)
          .map(vol =>
            s"""if [ -z $${${vol.variable}+x} ]; then
              |  ${vol.variable}=`pwd`; # todo: produce error here
              |fi""".stripMargin
          )
          .mkString("\n\n# provide temporary defaults for Docker\n", "\n", "")
      }

    // generate extra parsers for volumes
    val volumeParsers =
      if (volumes.getOrElse(Nil).isEmpty) {
        ""
      } else {
        volumes.getOrElse(Nil).map(vol =>
          s"""
            |${BashHelper.argStore("--" + vol.name, vol.variable, "\"$2\"", 2, storeVariable)}
            |${BashHelper.argStoreSed("--" + vol.name, vol.variable, storeVariable)}"""
        ).mkString
      }

    val setup =
      if (image == runImageName) { // if these are the same, then no dockerfile needs to be built
        s"docker pull $runImageName"
      } else {
        s"docker build -t $runImageName ."
      }
    s"""${BashHelper.quoteFunction}
      |
      |VIASHARGS=''
      |while [[ $$# -gt 0 ]]; do
      |    case "$$1" in
      |        ---setup)
      |            $setup
      |            exit 0
      |            ;;$volumeParsers
      |        *)    # unknown option
      |            ${BashHelper.quoteSaves("VIASHARGS", "$1")}
      |            shift # past argument
      |            ;;
      |    esac
      |done$volumeDefaults""".stripMargin
  }
}

case class Volume(
  name: String,
  mount: String
) {
  val variable = "VOLUME_" + name.toUpperCase()
}
