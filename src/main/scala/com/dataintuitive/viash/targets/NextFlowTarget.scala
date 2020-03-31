package com.dataintuitive.viash.targets

import com.dataintuitive.viash.functionality.{Functionality, Resource, StringObject}
import com.dataintuitive.viash.functionality.platforms.NativePlatform
import com.dataintuitive.viash.targets.environments._
import java.nio.file.Paths

import scala.reflect.ClassTag

/**
 * Target class for generating NextFlow (DSL2) modules.
 * Most of the functionality is derived from the DockerTarget and we fall back to it.
 * That also means the syntax needs to be compatible.
 *
 * Extra fields:
 * - executor: the type of 'process' to use, explicitly added to the source files for consistency
 */
case class NextFlowTarget(
  image: String,
  volumes: Option[List[Volume]] = None,
  port: Option[List[String]] = None,
  workdir: Option[String] = None,
  apt: Option[AptEnvironment] = None,
  r: Option[REnvironment] = None,
  python: Option[PythonEnvironment] = None,
  executor: Option[String]
) extends Target {
  val `type` = "nextflow"

  val dockerTarget = DockerTarget(image, volumes, port, workdir, apt, r, python)

  def modifyFunctionality(functionality: Functionality) = {

    val dockerFunctionality = dockerTarget.modifyFunctionality(functionality)

    val resourcesPath = "/app"

    def quote(str:String) = '"' + str + '"'

    // get main script/binary
    // Only the Native platform case is covered for the moment
    val mainResource = functionality.mainResource.get
    val mainPath = Paths.get(resourcesPath, mainResource.name).toFile().getPath()
    val executionCode = functionality.platform match {
      case Some(NativePlatform) => mainResource.path.getOrElse("echo No command provided")
      case _    => { println("Not implemented yet"); mainPath}
    }


    val asNestedTuples = ("params",
      List(
        (functionality.name,
          List(
            ("name", functionality.name),
            ("container", image),
            ("command", executionCode),
            ("arguments", functionality.inputs.map(x => (x.name, x.default.map(_.toString).getOrElse(quote("default")))))
          )
        )
      )
    )

    def mapToConfig(m:(String, Any), indent:String = ""):String = m match {
        case (k:String, v: List[(String, Any)]) =>
          indent + k + " {\n" + v.map(x => mapToConfig(x, indent + "  ")).mkString("\n") + "\n" + indent + "}"
        case (k:String, v: String) => indent + k + " = " + quote(v)
        case _ => indent + "Parsing ERROR - Not implemented yet " + m
    }

    val setup_nextflowconfig = Resource(
      name = "nextflow.config",
      code = Some(
        mapToConfig(asNestedTuples)
      )
    )

    val fname = functionality.name

    val setup_main_header = s"""nextflow.preview.dsl=2
        |import java.nio.file.Paths
        |
        """.stripMargin('|')

    val setup_main_utils = s"""
        |
        |def renderCLI(command, arguments) {
        |
        |  def argumentsList = []
        |  def argumentsMap = arguments
        |  argumentsMap.each{ it -> argumentsList << "--" + it.key + " " + it.value }
        |  def command_line = command + argumentsList
        |
        |  return command_line.join(" ")
        |
        |}
        """.stripMargin('|')

    val setup_main_process = s"""
        |
        |process simpleBashExecutor {
        |
        |  container "$${container}"
        |  publishDir "$${params.outDir}/$${sample}", mode: 'copy', overwrite: true
        |  input:
        |    tuple val(sample), path(input), val(output), val(container), val(cli)
        |  output:
        |    tuple val("$${sample}"), path("$${output}")
        |  script:
        |    \"\"\"
        |    $$cli
        |    \"\"\"
        |
        |}
        """.stripMargin('|')

    val setup_main_workflow = s"""
        |workflow $fname {
        |
        |    take:
        |    sample_input_params_
        |
        |    main:
        |
        |    key = "$fname"
        |
        |    def sample_input_output_function_cli_ =
        |        sample_input_params_.map{ sample, input, _params ->
        |            def defaultParams = params[key] ? params[key] : [:]
        |            def overrideParams = _params[key] ? _params[key] : [:]
        |            def updtParams = defaultParams + overrideParams
        |            println(updtParams)
        |            new Tuple5(
        |                sample,
        |                input,
        |                Paths.get(updtParams.arguments.output),
        |                updtParams.container,
        |                renderCLI([updtParams.command], updtParams.arguments)
        |            )
        |        }
        |
        |    simpleBashExecutor(sample_input_output_function_cli_)
        |
        |}
        """.stripMargin('|')

    val setup_main_entrypoint = s"""
        |workflow {
        |
        |   sample = "testSample"
        |   inputPath = Paths.get(params.$fname.arguments.input)
        |   ch_ = Channel.from(inputPath).map{ s -> new Tuple3(s, inputPath, params.$fname)}
        |
        |   $fname(ch_)
        |}
        """.stripMargin('|')

    val setup_main = Resource(
      name = "main.nf",
      code = Some(setup_main_header + setup_main_utils + setup_main_process + setup_main_workflow + setup_main_entrypoint)
    )

    dockerFunctionality.copy(
        resources =
          dockerFunctionality.resources ::: List(setup_nextflowconfig, setup_main)
    )
  }

}
