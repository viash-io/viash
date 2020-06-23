package com.dataintuitive.viash.targets

import com.dataintuitive.viash.functionality._
import com.dataintuitive.viash.functionality.dataobjects._
import com.dataintuitive.viash.functionality.resources._
import com.dataintuitive.viash.targets.environments._
import java.nio.file.Paths
import com.dataintuitive.viash.helpers.BashHelper
import com.dataintuitive.viash.functionality.resources.Resource

case class DockerTarget(
  image: String,
  target_image: Option[String] = None,
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

    // collect variables
    val dockerArgs = generateDockerRunArgs(functionality)

    // create setup
    val (imageName, setupCommands) = processDockerSetup(functionality, resourcesPath)

    // make commands
    val executor = s"""docker run $dockerArgs $imageName"""
    val debuggor = s"""docker run $dockerArgs -v `pwd`:/pwd --workdir /pwd -t $imageName sh"""

    // process docker mounts
    val (volPreParse, volParsers, volPostParse, volInputs) = processDockerVolumes(functionality)

    // add docker debug flag
    val (debPreParse, debParsers, debPostParse, debInputs) = addDockerDebug(debuggor)

    // add extra arguments to the functionality file for each of the volumes
    val fun2 = functionality.copy(
      arguments = functionality.arguments ::: volInputs ::: debInputs
    )

    // create new bash script
    val bashScript = BashScript(
        name = Some(functionality.name),
        text = Some(BashHelper.wrapScript(
          executor = executor,
          functionality = fun2,
          resourcesPath = "/resources",
          setupCommands = setupCommands,
          preParse = volPreParse + debPreParse,
          parsers = volParsers + debParsers,
          postParse = volPostParse + debPostParse,
          postRun = ""
        )),
        is_executable = true
      )

    fun2.copy(
      resources = bashScript :: fun2.resources.tail
    )
  }

  def processDockerSetup(functionality: Functionality, resourcesPath: String) = {
    // get dependencies
    val aptInstallCommands = apt.map(_.getInstallCommands()).getOrElse(Nil)
    val rInstallCommands = r.map(_.getInstallCommands()).getOrElse(Nil)
    val pythonInstallCommands = python.map(_.getInstallCommands()).getOrElse(Nil)

    val deps = List(aptInstallCommands, rInstallCommands, pythonInstallCommands).flatten

    // if no extra dependencies are needed, the provided image can just be used,
    // otherwise need to construct a separate docker container
    if (deps.isEmpty) {
      (image, s"docker image inspect $image >/dev/null 2>&1 || docker pull $image")
    } else {
      val imageName = target_image.getOrElse("viash_autogen/" + functionality.name)

      val runCommands = List(aptInstallCommands, rInstallCommands, pythonInstallCommands)

      val dockerFile =
        s"FROM $image\n" +
          runCommands.map(li => if (li.isEmpty) "" else li.mkString("RUN ", " && \\\n  ", "\n")).mkString("\n")

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
    val volStr = volumesGet.map(vol => s"""-v "$$${vol.variable}":"${vol.mount}" """).mkString("")

    // check whether entrypoint should be set to bash
    val entrypointStr = functionality.mainScript match {
      case Some(e: Executable) => "--entrypoint='' "
      case _ => "--entrypoint=sh "
    }

    portStr + volStr + entrypointStr + "-i --rm -v \"$RESOURCES_DIR\":/resources"
  }

  def processDockerVolumes(functionality: Functionality) = {
    val storeVariable = functionality.mainScript match {
      case Some(e: Executable) => None
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
        required = true,
        direction = Input
      )
    )

    (preParse, parsers, postParse, inputs)
  }

  def addDockerDebug(debugCommand: String) = {
    val preParse = ""
    val parsers = "\n" + BashHelper.argStore("---debug", "VIASHDEBUG", "yes", 1, None)
    val postParse =
      s"""
        |
        |# if desired, enter a debug session
        |if [ $${VIASHDEBUG} ]; then
        |  $debugCommand
        |  exit 0
        |fi"""

    val inputs = Nil

    (preParse, parsers, postParse, inputs)
  }
}

case class Volume(
  name: String,
  mount: String
) {
  private val VolumePattern = "^[A-Za-z_]*$".r

  require(
    VolumePattern.findFirstIn(name).isDefined,
    message = s"Volume $name: Should only consist of characters [A-Za-z_]."
  )

  val variable = "VOLUME_" + name.toUpperCase()
}
