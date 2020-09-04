package com.dataintuitive.viash.platforms

import com.dataintuitive.viash.functionality._
import com.dataintuitive.viash.functionality.dataobjects._
import com.dataintuitive.viash.functionality.resources._
import com.dataintuitive.viash.platforms.requirements._
import java.nio.file.Paths
import com.dataintuitive.viash.helpers.{BashHelper, BashWrapper}
import com.dataintuitive.viash.functionality.resources.Resource
import com.dataintuitive.viash.config.Version

case class DockerPlatform(
  id: String = "docker",
  image: String,
  version: Option[Version] = None,
  target_image: Option[String] = None,
  resolve_volume: ResolveVolume = Automatic,
  port: Option[List[String]] = None,
  workdir: Option[String] = None,
  apk: Option[ApkRequirements] = None,
  apt: Option[AptRequirements] = None,
  r: Option[RRequirements] = None,
  python: Option[PythonRequirements] = None,
  docker: Option[DockerRequirements] = None
) extends Platform {
  val `type` = "docker"

  case class Mods(
    preParse: String = "",
    parsers: String = "",
    postParse: String = "",
    postRun: String = "",
    inputs: List[DataObject[_]] = Nil,
    extraParams: String = ""
  ) {
    def `++`(dm: Mods) = {
      Mods(
        preParse = preParse + dm.preParse,
        parsers = parsers + dm.parsers,
        postParse = postParse + dm.postParse,
        postRun = postRun + dm.postRun,
        inputs = inputs ::: dm.inputs,
        extraParams = extraParams + dm.extraParams
      )
    }
  }

  val requirements: List[Requirements] =
    apk.toList :::
    apt.toList :::
    r.toList :::
    python.toList :::
    docker.toList

  def modifyFunctionality(functionality: Functionality) = {
    val resourcesPath = "/app"

    // collect variables
    val dockerArgs = generateDockerRunArgs(functionality)

    // create setup
    val (imageName, imageVersion, setupCommands) = processDockerSetup(functionality, resourcesPath)

    // generate automount code
    val dmVol = processDockerVolumes(functionality)

    // add ---debug flag
    val debuggor = s"""docker run --entrypoint=bash $dockerArgs -v `pwd`:/pwd --workdir /pwd -t $imageName:$imageVersion"""
    val dmDebug = addDockerDebug(debuggor)

    // add ---chown flag
    val dmChown = addDockerChown(functionality, dockerArgs, dmVol.extraParams, imageName, imageVersion)

    val dmDockerfile = Mods(
      parsers = """
        |        ---dockerfile)
        |            ViashDockerfile
        |            exit 0
        |            ;;""".stripMargin
      )

    // compile modifications
    val dm = dmVol ++ dmDebug ++ dmChown ++ dmDockerfile

    // make commands
    val entrypointStr = functionality.mainScript.get match {
      case s: Executable => "--entrypoint='' "
      case _ => "--entrypoint=bash "
    }
    val executor = s"""eval docker run $entrypointStr$dockerArgs${dm.extraParams} $imageName:$imageVersion"""

    // add extra arguments to the functionality file for each of the volumes
    val fun2 = functionality.copy(
      arguments = functionality.arguments ::: dm.inputs
    )

    // create new bash script
    val bashScript = BashScript(
        name = Some(functionality.name),
        text = Some(BashWrapper.wrapScript(
          executor = executor,
          functionality = fun2,
          resourcesPath = "/resources",
          setupCommands = setupCommands,
          preParse = dm.preParse,
          parsers = dm.parsers,
          postParse = dm.postParse,
          postRun = dm.postRun
        )),
        is_executable = true
      )

    fun2.copy(
      resources = Some(bashScript :: fun2.resources.getOrElse(Nil).tail)
    )
  }

  private val tagRegex = "(.*):(.*)".r

  def processDockerSetup(functionality: Functionality, resourcesPath: String) = {
    // get dependencies
    val runCommands = requirements.flatMap(_.dockerCommands)

    // if no extra dependencies are needed, the provided image can just be used,
    // otherwise need to construct a separate docker container
    if (runCommands.isEmpty) {
      val (imageName, tag) =
        image match {
          case tagRegex(imageName, tag) => (imageName, tag)
          case _ => (image, "latest")
        }
      (imageName, tag, s"docker image inspect $imageName:$tag >/dev/null 2>&1 || docker pull $imageName:$tag")
    } else {
      val imageName = target_image.getOrElse("viash_autogen/" + functionality.name)
      val imageVersion = version.map(_.toString).getOrElse("latest")

      val dockerFile =
        s"FROM $image\n\n" +
          docker.flatMap(_.dockerCommandsAtBegin.map(_ + "\n")).getOrElse("") +
          runCommands.mkString("\n")

      val buildArgs = docker.map(_.build_args.map(" --build-arg " + _).mkString).getOrElse("")

      val setupCommands =
        s"""function ViashDockerfile {
          |# Print Dockerfile contents to stdout
          |cat << 'VIASHDOCKER'
          |$dockerFile
          |VIASHDOCKER
          |}
          |
          |function ViashSetup {
          |# create temporary directory to store temporary dockerfile in
          |tmpdir=$$(mktemp -d /tmp/viash_setupdocker-${functionality.name}-XXXXXX)
          |function clean_up {
          |  rm -rf "\\$$tmpdir"
          |}
          |trap clean_up EXIT
          |ViashDockerfile > $$tmpdir/Dockerfile
          |if [ ! -z $$(docker images -q $imageName:$imageVersion) ]; then
          |  echo "Image consists locally or on DockerHub"
          |else
          |  # Quick workaround to have the resources available in the current dir
          |  cp $$VIASH_RESOURCES_DIR/* $$tmpdir
          |  # Build the container
          |  docker build -t $imageName:$imageVersion$buildArgs $$tmpdir
          |fi
          |}""".stripMargin

      (imageName, imageVersion, setupCommands)
    }
  }

  def generateDockerRunArgs(functionality: Functionality) = {
    // process port parameter
    val portStr = port.getOrElse(Nil).map("-p " + _ + " ").mkString("")

    portStr + "-i --rm -v \"$VIASH_RESOURCES_DIR\":/resources"
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
        "\n\n# detect volumes from file arguments" +
        args.filter(a => a.isInstanceOf[FileObject])
          .map(arg => {

            // resolve arguments with multiplicity different from
            // singular args
            if (arg.multiple) {
              val viash_temp = "VIASH_TEST_" + arg.plainName.toUpperCase()
              s"""
                |if [ ! -z "$$${arg.VIASH_PAR}" ]; then
                |  IFS="${arg.multiple_sep}"
                |  for var in $$${arg.VIASH_PAR}; do
                |    VIASH_EXTRA_MOUNTS="$$VIASH_EXTRA_MOUNTS $$(ViashAutodetectMountArg "$$var")"
                |    ${BashWrapper.store(viash_temp, "\"$(ViashAutodetectMount \"$var\")\"", Some(arg.multiple_sep)).mkString("\n    ")}
                |  done
                |  unset IFS
                |  ${arg.VIASH_PAR}="$$$viash_temp"
                |fi""".stripMargin
            } else {
              s"""
                |if [ ! -z "$$${arg.VIASH_PAR}" ]; then
                |  VIASH_EXTRA_MOUNTS="$$VIASH_EXTRA_MOUNTS $$(ViashAutodetectMountArg "$$${arg.VIASH_PAR}")"
                |  ${arg.VIASH_PAR}=$$(ViashAutodetectMount "$$${arg.VIASH_PAR}")
                |fi""".stripMargin
            }
          })
          .mkString("")
      } else {
        ""
      }

    Mods(
      preParse = preParse,
      parsers = parsers,
      postParse = postParse,
      extraParams = extraParams
    )
  }

  def addDockerDebug(debugCommand: String) = {
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


    Mods(
      parsers = parsers,
      postParse = postParse
    )
  }

  def addDockerChown(functionality: Functionality, dockerArgs: String, volExtraParams: String, imageName: String, imageVersion: String) = {
    val chownVar = "VIASH_CHOWN"

    val args = functionality.arguments

    def chownCommand(value: String) = {
      s"""eval docker run --entrypoint=chown $dockerArgs$volExtraParams $imageName:$imageVersion $$$chownVar -R $value"""
    }

    val parsers =
        s"""        ---chown)
           |            $chownVar="$$(id -u):$$(id -g)"
           |            shift 1
           |            ;;""".stripMargin

    val postRun =
      "\n\n# change file ownership" +
      args
        .filter(a => a.isInstanceOf[FileObject] && a.direction == Output)
        .map(arg => {

          // resolve arguments with multiplicity different from
          // singular args
          if (arg.multiple) {
            val viash_temp = "VIASH_TEST_" + arg.plainName.toUpperCase()
            s"""
              |if [ ! -z "$$$chownVar" ] && [ ! -z "$$${arg.VIASH_PAR}" ]; then
              |  IFS="${arg.multiple_sep}"
              |  for var in $$${arg.VIASH_PAR}; do
              |    ${chownCommand("\"$var\"")}
              |  done
              |  unset IFS
              |  ${arg.VIASH_PAR}="$$$viash_temp"
              |fi""".stripMargin
          } else {
            s"""
              |if [ ! -z "$$$chownVar" ] && [ ! -z "$$${arg.VIASH_PAR}" ]; then
              |  ${chownCommand("\"$" + arg.VIASH_PAR + "\"")}
              |fi""".stripMargin
          }
        })
        .mkString("")

    Mods(
      parsers = parsers,
      postRun = postRun
    )
  }
}
