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

package io.viash.platforms

import io.viash.config.Config
import io.viash.functionality._
import io.viash.functionality.arguments._
import io.viash.functionality.resources._
import io.viash.platforms.requirements._
import io.viash.helpers.{Bash, Docker}
import io.viash.config.Version
import io.viash.wrapper.{BashWrapper, BashWrapperMods}
import io.viash.platforms.docker._
import io.viash.helpers.Circe._
import io.viash.config.Info
import java.util.Date
import java.text.SimpleDateFormat
import io.viash.helpers.description
import io.viash.helpers.example
import io.viash.helpers.deprecated

@description("""Run a Viash component on a Docker backend platform.
               |By specifying which dependencies your component needs, users will be able to build a docker container from scratch using the setup flag, or pull it from a docker repository.
               |""".stripMargin)
case class DockerPlatform(
  @description("As with all platforms, you can give a platform a different name. By specifying `id: foo`, you can target this platform (only) by specifying `-p foo` in any of the Viash commands.")
  @example("id: foo", "yaml")
  id: String = "docker",

  @description("The base container to start from. You can also add the tag here if you wish.")
  @example("image: \"bash:4.0\"", "yaml")
  image: String,

  @description("Name of a container’s [organization](https://docs.docker.com/docker-hub/orgs/).")
  organization: Option[String],

  @description("The URL to the a [custom Docker registry](https://docs.docker.com/registry/)")
  @example("registry: https://my-docker-registry.org", "yaml")
  registry: Option[String] = None,

  @description("Specify a Docker image based on its tag.")
  @example("tag: 4.0", "yaml")
  tag: Option[Version] = None,
  
  @description("If anything is specified in the setup section, running the `---setup` will result in an image with the name of `<target_image>:<version>`. If nothing is specified in the `setup` section, simply `image` will be used.")
  @example("target_image: myfoo", "yaml")
  target_image: Option[String] = None,
  target_organization: Option[String] = None,

  @description("The URL where the resulting image will be pushed to.")
  @example("target_registry: https://my-docker-registry.org", "yaml")
  target_registry: Option[String] = None,

  @description("The tag the resulting image gets.")
  @example("target_tag: 0.5.0", "yaml")
  target_tag: Option[Version] = None,

  @description("The default namespace separator is \"_\".")
  @example("namespace_separator: \"+\"", "yaml")
  namespace_separator: String = "_",
  resolve_volume: DockerResolveVolume = Automatic,

  @description("In Linux, files created by a Docker container will be owned by `root`. With `chown: true`, Viash will automatically change the ownership of output files (arguments with `type: file` and `direction: output`) to the user running the Viash command after execution of the component. Default value: `true`.")
  @example("chown: false", "yaml")
  chown: Boolean = true,

  @description("A list of enabled ports. This doesn’t change the Dockerfile but gets added as a command-line argument at runtime.")
  @example("""port:
             |  - 80
             |  - 8080
             |""".stripMargin, "yaml")
  port: OneOrMore[String] = Nil,

  @description("The working directory when starting the container. This doesn’t change the Dockerfile but gets added as a command-line argument at runtime.")
  @example("workdir: /home/user", "yaml")
  workdir: Option[String] = None,
  setup_strategy: DockerSetupStrategy = IfNeedBePullElseCachedBuild,
  privileged: Boolean = false,

  @description("Add [docker run](https://docs.docker.com/engine/api/commandline/run/) arguments.")
  run_args: OneOrMore[String] = Nil,

  @description("The source of the target image. This is used for defining labels in the dockerfile.")
  @example("target_image_source: https://github.com/foo/bar", "yaml")
  target_image_source: Option[String] = None,
  `type`: String = "docker",

  // setup variables
  @description("""A list of requirements for installing the following types of packages:
                 |
                 | - apt
                 | - apk
                 | - yum
                 | - R
                 | - Python
                 | - JavaScript
                 | - Docker setup instructions
                 |
                 |The order in which these dependencies are specified determines the order in which they will be installed.
                 |""".stripMargin)
  setup: List[Requirements] = Nil,

  @description("Specify which apk packages should be available in order to run the component.")
  @example("""setup:
             |  - type: apk
             |    packages: [ sl ]
             |""".stripMargin, "yaml")
  @deprecated("Use `setup` instead.", "Viash 0.5.15")
  apk: Option[ApkRequirements] = None,

  @description("Specify which apt packages should be available in order to run the component.")
  @example("""setup:
             |  - type: apt
             |    packages: [ sl ]
             |""".stripMargin, "yaml")
  @deprecated("Use `setup` instead.", "Viash 0.5.15")
  apt: Option[AptRequirements] = None,

  @description("Specify which yum packages should be available in order to run the component.")
  @example("""setup:
             |  - type: yum
             |    packages: [ sl ]
             |""".stripMargin, "yaml")
  @deprecated("Use `setup` instead.", "Viash 0.5.15")
  yum: Option[YumRequirements] = None,

  @description("Specify which R packages should be available in order to run the component.")
  @example("""setup: 
             |  - type: r
             |    cran: [ dynutils ]
             |    bioc: [ AnnotationDbi ]
             |    git: [ https://some.git.repository/org/repo ]
             |    github: [ rcannood/SCORPIUS ]
             |    gitlab: [ org/package ]
             |    svn: [ https://path.to.svn/group/repo ]
             |    url: [ https://github.com/hadley/stringr/archive/HEAD.zip ]
             |    script: [ 'devtools::install(".")' ]
             |""".stripMargin, "yaml")
  @deprecated("Use `setup` instead.", "Viash 0.5.15")
  r: Option[RRequirements] = None,

  @description("Specify which Python packages should be available in order to run the component.")
  @example("""setup:
             |  - type: python
             |    pip: [ numpy ]
             |    git: [ https://some.git.repository/org/repo ]
             |    github: [ jkbr/httpie ]
             |    gitlab: [ foo/bar ]
             |    mercurial: [ http://... ]
             |    svn: [ http://...]
             |    bazaar: [ http://... ]
             |    url: [ http://... ]
             |""".stripMargin, "yaml")
  @deprecated("Use `setup` instead.", "Viash 0.5.15")
  python: Option[PythonRequirements] = None,

  @description("Specify which Docker commands should be run during setup.")
  @example("""setup:
             |  - type: docker
             |    build_args: [ GITHUB_PAT=hello_world ]
             |    run: [ git clone ... ]
             |    add: [ "http://foo.bar ." ]
             |    copy: [ "http://foo.bar ." ]
             |    resources: 
             |      - resource.txt /path/to/resource.txt
             |""".stripMargin, "yaml")
  @deprecated("Use `setup` instead.", "Viash 0.5.15")
  docker: Option[DockerRequirements] = None,
  test_setup: List[Requirements] = Nil
) extends Platform {
  override val hasSetup = true

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


  def modifyFunctionality(config: Config, testing: Boolean): Functionality = {
    val functionality = config.functionality
    // collect docker args
    val dockerArgs = "-i --rm" +
      port.map(" -p " + _).mkString +
      run_args.map(" " + _).mkString +
      { if (privileged) " --privileged" else "" }

    // create setup
    val (effectiveID, setupMods) = processDockerSetup(functionality, config.info, testing)

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

  private def processDockerSetup(functionality: Functionality, info: Option[Info], testing: Boolean) = {
    // construct labels from metadata
    val opencontainers_image_authors = functionality.authors match {
      case Nil => None
      case aut: List[Author] => Some(aut.mkString(", "))
    }
    // if no target_image_source is defined,
    // translate git@github.com:viash-io/viash.git -> https://github.com/viash-io/viash.git
    val opencontainers_image_source = (target_image_source, info) match {
      case (Some(tis), _) => Some(tis)
      case (None, Some(i)) =>
        i.git_remote.map(url => url.replaceAll(":([^/])", "/$1").replaceAllLiterally("ssh//", "").replaceAllLiterally("git@", "https://"))
      case _ => None
    }
    val opencontainers_image_revision = info.flatMap(_.git_commit)
    val opencontainers_image_version = functionality.version.map(v => v.toString())
    val opencontainers_image_description = s""""Companion container for running component ${functionality.namespace.map(_ + " ").getOrElse("")}${functionality.name}""""
    val opencontainers_image_created = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").format(new Date())

    val authors = opencontainers_image_authors.map(aut => s"""org.opencontainers.image.authors="$aut"""").toList
    val descr = List(s"org.opencontainers.image.description=$opencontainers_image_description")
    val imageSource = opencontainers_image_source.map(des => s"""org.opencontainers.image.source="${Bash.escape(des)}"""").toList
    val created = List(s"""org.opencontainers.image.created="$opencontainers_image_created"""")
    val revision = opencontainers_image_revision.map(rev => s"""org.opencontainers.image.revision="$rev"""").toList
    val version = opencontainers_image_version.map(v => s"""org.opencontainers.image.version="$v"""").toList
    val labelReq = DockerRequirements(label = authors ::: descr ::: created ::: imageSource ::: revision ::: version)

    val setupRequirements = testing match {
      case true => test_setup
      case _ => Nil
    }
    val requirements2 = requirements ::: setupRequirements ::: List(labelReq)

    // get dependencies
    val runCommands = requirements2.flatMap(_.dockerCommands)


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
    val args = functionality.allArgumentsAndDummies

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
            case arg: FileArgument if arg.multiple =>
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
            case arg: FileArgument =>
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
         |${BashWrapper.var_executable}=$$(ViashAutodetectMount "$$${BashWrapper.var_executable}")
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
    val args = functionality.allArgumentsAndDummies

    def chownCommand(value: String): String = {
      s"""eval docker run --entrypoint=chown $dockerArgs$volExtraParams $fullImageID "$$(id -u):$$(id -g)" --silent --recursive $value"""
    }

    val preRun =
      if (chown) {
        // chown output files/folders
        val chownPars = args
          .filter(a => a.isInstanceOf[FileArgument] && a.direction == Output)
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
