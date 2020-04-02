package com.dataintuitive.viash.targets

import com.dataintuitive.viash.functionality.{Functionality, Resource, StringObject}
import com.dataintuitive.viash.functionality.platforms.NativePlatform
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

    // construct execute resources
    val runImageName = if (dockerFile.isEmpty) image else "somename"

    val portStr = port.getOrElse(Nil).map("-p " + _ + " ").mkString("")

    val volumesGet = volumes.getOrElse(Nil)
    val volParse =
      if (volumesGet.isEmpty) {
        ""
      } else {
        val volStrs =
          volumesGet.map(vol =>
            s"""
              |    ---${vol.name})
              |    ${vol.name.toUpperCase()}="$$2"
              |    POSITIONAL+=("$$1") # save it in an array for later
              |    shift # past argument
              |    POSITIONAL+=("$$1") # save it in an array for later
              |    shift # past value
              |    ;;""".stripMargin
          ).mkString("")
        val fillIns = 
          volumesGet.map(vol =>
            s"""
              |if [ -z $${${vol.name.toUpperCase()}+x} ]; then 
              |  ${vol.name.toUpperCase()}=`pwd`; 
              |fi""".stripMargin
          ).mkString("")

        s"""
          |POSITIONAL=()
          |ADDITIONAL=()
          |while [[ $$# -gt 0 ]]
          |do
          |key="$$1"
          |
          |case $$key in$volStrs
          |    *)    # unknown option
          |    POSITIONAL+=("$$1") # save it in an array for later
          |    ADDITIONAL+=("$$1") # save it in an array for later
          |    shift # past argument
          |    ;;
          |esac
          |done
          |
          |set -- "$${POSITIONAL[@]}" # restore positional parameters
          |
          |# provide temporary defaults for Docker$fillIns
          |""".stripMargin
      }
    val volStr = volumesGet.map(vol => s"-v $$${vol.name.toUpperCase()}:${vol.mount} ").mkString("")
    val volInputs = volumesGet.map(vol => 
      StringObject(
        name = Some(vol.name), 
        description = Some(s"Local path to mount directory for volume '${vol.name}'."),
        required = Some(true)
      )
    )

    // create new fun with extra params
    val fun2 = functionality.copy(
      inputs = functionality.inputs ::: volInputs
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
        mainResource.path.map(_ + " \"${ADDITIONAL[@]}\"").getOrElse("echo No command provided")
      case Some(pl) => {
        val code = fun2.mainCodeWithArgParse.get

        s"""
        |if [ ! -d "$resourcesPath" ]; then mkdir "$resourcesPath"; fi
        |cat > "$mainPath" << 'VIASHMAIN'
        |$code
        |VIASHMAIN
        |${pl.command(mainPath)} $$@
        |""".stripMargin
      }
    }

    val heredocStart = if (executionCode.contains("\n")) { "cat << VIASHEOF | " } else { "" }
    val heredocEnd = if (executionCode.contains("\n")) { "\nVIASHEOF" } else { "" }
    val execute_bash = 
      Resource(
        name = "execute.sh",
        code = Some(s"""#!/bin/bash
          |
          |$volParse
          |
          |${heredocStart}docker run -i $volStr$portStr$runImageName $executionCode$heredocEnd
        """.stripMargin),
        isExecutable = Some(true)
      )

//    val execute_batch = Resource(
//      name = "execute.bat",
//      code = Some(s"TODO")
//    )

    // construct setup resources
    val setup_bash = Resource(
      name = "setup.sh",
      code = {
        if (dockerFile.isEmpty) {
          Some(s"""#!/bin/bash\n\ndocker pull $runImageName""")
        } else {
          Some(s"""#!/bin/bash
            |
            |docker build -t $runImageName .
            """.stripMargin)
        }
      },
      isExecutable = Some(true)
    )

//    val setup_batch = Resource(
//      name = "setup.bat",
//      code = {
//        if (dockerFile.isEmpty) {
//          Some(s"docker pull $runImageName")
//        } else {
//          Some(s"""docker build -t $runImageName .""")
//        }
//      }
//    )

    fun2.copy(
      resources =
        fun2.resources.filterNot(_.name.startsWith("main")) ::: 
        dockerFile :::
//        List(execute_bash, execute_batch, setup_bash, setup_batch)
        List(execute_bash, setup_bash)
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
            } +
            s"\nENTRYPOINT sh\n"
        )
      ))
    }
  }
}

case class Volume(
  name: String,
  mount: String
)
