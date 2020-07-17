package com.dataintuitive.viash.targets

import com.dataintuitive.viash.functionality._
import com.dataintuitive.viash.functionality.dataobjects._
import com.dataintuitive.viash.functionality.resources._
import com.dataintuitive.viash.targets.environments._
import java.nio.file.Paths
import com.dataintuitive.viash.helpers.{BashHelper, BashWrapper}
import com.dataintuitive.viash.functionality.resources.Resource

case class DockerTarget(
  image: String,
  target_image: Option[String] = None,
  resolve_volume: ResolveVolume = Automatic,
  port: Option[List[String]] = None,
  workdir: Option[String] = None,
  apk: Option[ApkEnvironment] = None,
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

    // process docker mounts
    val (volPreParse, volParsers, volPostParse, volInputs, volExtraParams) = processDockerVolumes(functionality)

    // add docker debug flag
    val debuggor = s"""docker run --entrypoint=bash $dockerArgs -v `pwd`:/pwd --workdir /pwd -t $imageName"""
    val (debPreParse, debParsers, debPostParse, debInputs) = addDockerDebug(debuggor)

    // make commands
    val entrypointStr = functionality.mainScript.get match {
      case s: Executable => "--entrypoint='' "
      case _ => "--entrypoint=bash "
    }
    val executor = s"""eval docker run $entrypointStr$dockerArgs$volExtraParams $imageName"""

    // add extra arguments to the functionality file for each of the volumes
    val fun2 = functionality.copy(
      arguments = functionality.arguments ::: volInputs ::: debInputs
    )

    // create new bash script
    val bashScript = BashScript(
        name = Some(functionality.name),
        text = Some(BashWrapper.wrapScript(
          executor = executor,
          functionality = fun2,
          resourcesPath = "/resources",
          setupCommands = setupCommands,
          preParse = volPreParse + debPreParse,
          parsers = volParsers + debParsers,
          postParse = debPostParse + volPostParse,
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
    val apkInstallCommands = apk.map(_.getInstallCommands()).getOrElse(Nil)
    val rInstallCommands = r.map(_.getInstallCommands()).getOrElse(Nil)
    val pythonInstallCommands = python.map(_.getInstallCommands()).getOrElse(Nil)

    val runCommands = List(aptInstallCommands, apkInstallCommands, rInstallCommands, pythonInstallCommands)

    // if no extra dependencies are needed, the provided image can just be used,
    // otherwise need to construct a separate docker container
    if (runCommands.flatten.isEmpty) {
      (image, s"docker image inspect $image >/dev/null 2>&1 || docker pull $image")
    } else {
      val imageName = target_image.getOrElse("viash_autogen/" + functionality.name)

      val dockerFile =
        s"FROM $image\n" +
          runCommands.map(li => if (li.isEmpty) "" else li.mkString("RUN ", " && \\\n  ", "\n")).mkString("\n")

      val setupCommands =
        s"""# create temporary directory to store temporary dockerfile in
          |tmpdir=$$(mktemp -d /tmp/viash_setupdocker-${functionality.name}-XXXXXX)
          |function clean_up {
          |  rm -rf "\\$$tmpdir"
          |}
          |trap clean_up EXIT
          |cat > $$tmpdir/Dockerfile << 'VIASHDOCKER'
          |$dockerFile
          |VIASHDOCKER
          |docker build -t $imageName $$tmpdir""".stripMargin
      (imageName, setupCommands)
    }
  }

  def generateDockerRunArgs(functionality: Functionality) = {
    // process port parameter
    val portStr = port.getOrElse(Nil).map("-p " + _ + " ").mkString("")

    portStr + "-i --rm -v \"$RESOURCES_DIR\":/resources"
  }

  def processDockerVolumes(functionality: Functionality) = {
    val extraMountsVar = "VIASH_EXTRA_MOUNTS"

    val args = functionality.arguments

    val preParse =
      if (args.isEmpty) {
        ""
      } else if (resolve_volume == Automatic) {
        s"""${BashHelper.ViashAbsolutePath}
           |${BashHelper.ViashAutodetectMount}
           |${BashHelper.ViashExtractFlags}
           |# initialise autodetect mount variable
           |$extraMountsVar=''""".stripMargin
      } else {
        BashHelper.ViashExtractFlags
      }

    val parsers =
        s"""        ---v|---volume)
           |            ${BashHelper.save(extraMountsVar, Seq("-v \"$2\""))}
           |            shift 2
           |            ;;
           |        ---volume=*)
           |            ${BashHelper.save(extraMountsVar, Seq("-v $(ViashRemoveFlags \"$2\")"))}
           |            shift 1
           |            ;;""".stripMargin

    val extraParams = s" $$$extraMountsVar"

    val postParse =
      if (resolve_volume == Automatic) {
        "\n# detect volumes from file arguments\n" +
        args.filter(a => a.isInstanceOf[FileObject])
          .map(arg => {
            val viash_par = "VIASH_PAR_" + arg.plainName.toUpperCase()
            s"""
              |if [ ! -z "$$$viash_par" ]; then
              |  VIASH_EXTRA_MOUNTS="$$VIASH_EXTRA_MOUNTS $$(ViashAutodetectMountArg "$$$viash_par")"
              |  $viash_par=$$(ViashAutodetectMount "$$$viash_par")
              |fi""".stripMargin
          })
          .mkString("")
      } else {
        ""
      }

    val inputs = Nil

    (preParse, parsers, postParse, inputs, extraParams)
  }

  def addDockerDebug(debugCommand: String) = {
    val preParse = ""
    val parsers = "\n" + BashHelper.argStore("---debug", "VIASH_DEBUG", "yes", 1, None)
    val postParse =
      s"""
        |
        |# if desired, enter a debug session
        |if [ $${VIASH_DEBUG} ]; then
        |  echo "+ $debugCommand"
        |  $debugCommand
        |  exit 0
        |fi"""

    val inputs = Nil

    (preParse, parsers, postParse, inputs)
  }
}
