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

    // process docker mounts
    val (volPreParse, volParsers, volPostParse, volInputs) = processDockerVolumes(functionality)

    // create setup
    val (imageName, setupCommands) = processDockerSetup(functionality, resourcesPath)

    // add extra arguments to the functionality file for each of the volumes
    val fun2 = functionality.copy(
      arguments = functionality.arguments ::: volInputs
    )

    // collect variables
    val dockerArgs = generateDockerRunArgs(functionality)

    // create new bash script
    val bashScript = Resource(
        name = functionality.name,
        code = Some(BashHelper.wrapScript(
          executor = s"docker run $dockerArgs $imageName",
          functionality = fun2,
          resourcesPath = resourcesPath,
          setupCommands = setupCommands,
          preParse = volPreParse,
          parsers = volParsers,
          postParse = volPostParse,
          postRun = ""
        )),
        isExecutable = true
      )

    fun2.copy(
      resources = fun2.resources.filterNot(_.name.startsWith("main")) :::
        List(bashScript)
    )
  }

  def processDockerSetup(functionality: Functionality, resourcesPath: String) = {
    // get dependencies
    val aptInstallCommands = apt.map(_.getInstallCommands()).getOrElse(Nil)
    val rInstallCommands = r.map(_.getInstallCommands()).getOrElse(Nil)
    val pythonInstallCommands = python.map(_.getInstallCommands()).getOrElse(Nil)
    val resourceNames = functionality.resources.map(_.name).filterNot(_.startsWith("main"))

    val deps = List(aptInstallCommands, rInstallCommands, pythonInstallCommands, resourceNames).flatten

    // if no extra dependencies are needed, the provided image can just be used,
    // otherwise need to construct a separate docker container
    if (deps.isEmpty) {
      (image, s"docker pull $image")
    } else {
      val imageName = "viash_autogen/" + functionality.name

      val runCommands = List(aptInstallCommands, rInstallCommands, pythonInstallCommands)

      val dockerFile =
        s"FROM $image\n" +
          runCommands.map(li => if (li.isEmpty) "" else li.mkString("RUN ", " && \\\n  ", "\n")).mkString("\n") +
          {
            if (!resourceNames.isEmpty) {
              s"""COPY ${resourceNames.mkString(" ")} $resourcesPath/
                |WORKDIR $resourcesPath
                """.stripMargin
            } else {
              ""
            }
          }
      val setupCommands =
        s"""# create temporary directory to store temporary dockerfile in
          |tmpdir=$$(mktemp -d /tmp/viashdocker-${functionality.name}-XXXXXX)
          |cat > $$tmpdir/Dockerfile << 'VIASHDOCKER'
          |$dockerFile
          |VIASHDOCKER
          |docker build -t $imageName $$tmpdir
          |rm -r $$tmpdir""".stripMargin
      (imageName, setupCommands)
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
      case NativePlatform => ""
      case _ => "--entrypoint bash "
    }

    portStr + volStr + entrypointStr + "-i"
  }

  def processDockerVolumes(functionality: Functionality) = {
    val storeVariable = functionality.platform match {
      case NativePlatform => None
      case _ => Some("VIASHARGS")
    }

    val parsers =
      if (volumes.getOrElse(Nil).isEmpty) {
        ""
      } else {
        volumes.getOrElse(Nil).map(vol =>
          s"""
            |${BashHelper.argStore("--" + vol.name, vol.variable, "\"$2\"", 2, storeVariable)}
            |${BashHelper.argStoreSed("--" + vol.name, vol.variable, storeVariable)}"""
        ).mkString
      }

    val preParse = ""
    val postParse =
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

    val inputs = volumes.getOrElse(Nil).map(vol =>
      StringObject(
        name = "--" + vol.name,
        description = Some(s"Local path to mount directory for volume '${vol.name}'."),
        required = Some(true),
        direction = Input
      )
    )

    (preParse, parsers, postParse, inputs)
  }
}

case class Volume(
  name: String,
  mount: String
) {
  val variable = "VOLUME_" + name.toUpperCase()
}
