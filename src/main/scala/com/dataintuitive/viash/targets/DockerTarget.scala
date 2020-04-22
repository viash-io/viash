package com.dataintuitive.viash.targets

import com.dataintuitive.viash.functionality._
import com.dataintuitive.viash.functionality.platforms.{NativePlatform, BashPlatform}
import com.dataintuitive.viash.targets.environments._
import java.nio.file.Paths

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
        "\nset -- $VIASHARGS\n" + fun2.mainCodeWithArgParse.get.replaceAll("\\$", "\\\\\\$")
      case Some(pl) => {
        val code = fun2.mainCodeWithArgParse.get.replaceAll("\\$", "\\\\\\$")

        s"""
        |if [ ! -d "$resourcesPath" ]; then mkdir "$resourcesPath"; fi
        |cat > "$mainPath" << 'VIASHMAIN'
        |$code
        |VIASHMAIN
        |${pl.command(mainPath)} $$VIASHARGS
        |""".stripMargin
      }
    }

    // generate bash document
    // TODO: Maybe BashPlatform oneliners will fail here?
    val (heredocStart, heredocEnd) = if (executionCode.contains("\n")) {
      ("cat << VIASHEOF | ", "\nVIASHEOF")
    } else {
      ("", "")
    }

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
          |${heredocStart}docker run ${generateDockerRunArgs()} $runImageName $executionCode$heredocEnd""".stripMargin),
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

  def generateDockerRunArgs() = {
    // process port parameter
    val portStr = port.getOrElse(Nil).map("-p " + _ + " ").mkString("")

    // process volume parameter
    val volumesGet = volumes.getOrElse(Nil)
    val volStr = volumesGet.map(vol => s"-v $$${vol.name.toUpperCase()}:${vol.mount} ").mkString("")

    portStr + volStr + "-i --entrypoint bash"
  }

  def generateBashParsers(functionality: Functionality, runImageName: String) = {
    // helper function for quoting arguments before passing again
    def saveArgument(arg: String) = {
      s"""VIASHARGS="$$VIASHARGS '`echo $arg | sed \\\"s/'/'\\\\\\\"'\\\\\\\"'/g\\\"`'""""
    }

    // remove extra volume args if extra parameters are not desired
    // -> when the executable's cli does not accept the volume arguments
    val volumeArgFix = functionality.platform match {
      case None => "# "
      case Some(NativePlatform) => "# "
      case _ => ""
    }

    // generate volume checks
    val volumeDefaults =
      if (volumes.getOrElse(Nil).isEmpty) {
        ""
      } else {
        volumes.getOrElse(Nil)
          .map(vol =>
            s"""if [ -z $${${vol.name.toUpperCase()}+x} ]; then
              |  ${vol.name.toUpperCase()}=`pwd`; # todo: produce error here
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
            |        --${vol.name})
            |            ${vol.name.toUpperCase()}="$$2"
            |            ${volumeArgFix}${saveArgument("$1")}
            |            ${volumeArgFix}${saveArgument("$2")}
            |            shift 2 # past argument and value
            |            ;;
            |        --${vol.name}=*)
            |            ${vol.name.toUpperCase()}=`echo $$1 | sed 's/^--${vol.name}=//'`
            |            ${volumeArgFix}${saveArgument("$1")}
            |            shift # past argument
            |            ;;""".stripMargin
        ).mkString("")
      }

    s"""VIASHARGS=''
      |while [[ $$# -gt 0 ]]; do
      |    case "$$1" in
      |        ---setup)
      |            docker build -t $runImageName .
      |            exit 0
      |            ;;$volumeParsers
      |        *)    # unknown option
      |            ${saveArgument("$1")}
      |            shift # past argument
      |            ;;
      |    esac
      |done$volumeDefaults""".stripMargin
  }
}

case class Volume(
  name: String,
  mount: String
)
