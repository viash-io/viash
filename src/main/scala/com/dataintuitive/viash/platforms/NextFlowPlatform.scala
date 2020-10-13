package com.dataintuitive.viash.platforms

import com.dataintuitive.viash.functionality._
import com.dataintuitive.viash.functionality.resources._
import com.dataintuitive.viash.functionality.dataobjects._
import com.dataintuitive.viash.platforms.requirements._
import com.dataintuitive.viash.config.Version

/**
 * / * Platform class for generating NextFlow (DSL2) modules.
 */
case class NextFlowPlatform(
  id: String = "nextflow",
  version: Option[Version] = None,
  image: Option[String],
  executor: Option[String],
  publish: Option[Boolean],
  per_id: Option[Boolean],
  path: Option[String],
  label: Option[String],
  stageInMode: Option[String],

  // TODO: these parameters could (and should?) be removed
  // as they have no actual impact on anything
  apt: Option[AptRequirements] = None,
  r: Option[RRequirements] = None,
  python: Option[PythonRequirements] = None,
  setup: List[Requirements] = Nil
) extends Platform {
  val `type` = "nextflow"

  val requirements: List[Requirements] = Nil

  private val nativePlatform = NativePlatform(id = id, version = version, r = r, python = python, setup = setup)

  def modifyFunctionality(functionality: Functionality): Functionality = {
    import NextFlowUtils._
    implicit val fun: Functionality = functionality

    val fname = functionality.name

    // get main script/binary
    val mainResource = functionality.mainScript
    val executionCode = mainResource match {
      case Some(e: Executable) => e.path.get
      case _ => fname
    }

    val allPars = functionality.arguments

    def inputFileExtO = allPars
      .filter(_.`type` == "file")
      .find(_.direction == Input)
      .flatMap(_.default.map(_.toString.split('.').last))

    def outputFileExtO = allPars
      .filter(_.`type` == "file")
      .find(_.direction == Output)
      .flatMap(_.default.map(_.toString.split('.').last))

    // All values for arguments/parameters are defined in the root of
    // the params structure. the function name is prefixed as a namespace
    // identifier. A "__" is used to separate namespace and arg/option.

    val namespacedParameters =
      functionality.arguments.map { dataObject =>
        namespacedValueTuple(
          dataObject.plainName.replace("-", "_"),
          dataObject.default.map(_.toString).getOrElse("value_not_found")
        )
      }

    val argumentsAsTuple =
      if (functionality.arguments.nonEmpty) {
        List(
          "arguments" → functionality.arguments.map(_.toTuple)
        )
      } else {
        Nil
      }

    val extensionsAsTuple = outputFileExtO match {
      case Some(ext) => List(
        "extensions" → List(("out", ext))
      )
      case None => Nil
    }

    val imageName = {
      val autogen = functionality.namespace.map( _ + "/" + functionality.name).getOrElse(functionality.name)
      image.getOrElse(autogen)
    }

    /**
     * A few notes:
     * 1. input and output are initialized as empty strings, so that no warnings appear.
     * 2. id is initialized as empty string, which makes sense in test scenarios.
     */
    val asNestedTuples: List[(String, Any)] = List(
      "docker.enabled" → true,
      "process.container" → "dataintuitive/portash",
      "params" → {
        namespacedParameters :::
          List(
            "id" → "",
            "dockerPrefix" -> "",
            "input" → "",
            "output" → "",
            functionality.name → {
              List(
                "name" → functionality.name,
                "container" → (imageName + ":" + version.map(_.toString).getOrElse("latest")),
                "command" → executionCode
              ) :::
                extensionsAsTuple :::
                argumentsAsTuple
            }
          )
      }
    )

    val setup_nextflowconfig = PlainFile(
      name = Some("nextflow.config"),
      text = Some(listMapToConfig(asNestedTuples))
    )

    val setup_main_header =
      s"""nextflow.preview.dsl=2
         |import java.nio.file.Paths
         |if (!params.containsKey("input") || params.input == "") {
         |    exit 1, "ERROR: Please provide a --input parameter containing an input file/dir or a wildcard expression"
         |}
         |if (!params.containsKey("output") || params.output == "" ) {
         |    exit 1, "ERROR: Please provide a --output parameter for storing the output"
         |}
         |""".stripMargin

    val setup_main_utils =
      s"""
         |def renderCLI(command, arguments) {
         |
         |    def argumentsList = arguments.collect{ it ->
         |        (it.otype == "")
         |            ? "\\'" + it.value + "\\'"
         |            : (it.type == "boolean_true")
         |                ? it.otype + it.name
         |                : (it.value == "")
         |                    ? ""
         |                    : it.otype + it.name + " \\'" + ((it.value in List && it.multiple) ? it.value.join(it.multiple_sep): it.value) + "\\'"
         |    }
         |
         |    def command_line = command + argumentsList
         |
         |    return command_line.join(" ")
         |}
         |""".stripMargin

    /**
     * What should the output filename be, in terms of the input?
     * This is irrelevant for simple one-step function calling, but it is crucial in a pipeline.
     * This uses the function type, but there is no check on it yet!
     * TODO: Check for conditions
     */
    val setup_main_outFromIn = functionality.function_type match {
      // in and out file format are the same, but also the filenames!
      case Some(AsIs) => """
                          |def getOutputFilename(_params) {
                          |
                          |    def output = _params.arguments.find{it ->
                          |      (it.value.direction == "Output" && it.value.type == "file")
                          |    }
                          |
                          |    return output.value.value
                          |}
                          |""".stripMargin.replace("__e__", inputFileExtO.getOrElse("OOPS")).replace("__f__", fname)
      // Out format is different from in format
      case Some(Convert) | Some(Join) | None => """
                                                  |// files is either String, List[String] or HashMap[String,String]
                                                  |def outFromIn(files) {
                                                  |    if (files in List || files in HashMap) {
                                                  |        // We're in join mode, files is List[String]
                                                  |        return "__f__" + "." + __e__
                                                  |    } else {
                                                  |        // files filename is just a String
                                                  |        def splitString = files.split(/\./)
                                                  |        def prefix = splitString.head()
                                                  |        def extension = splitString.last()
                                                  |        return prefix + "." + "__f__" + "." + __e__
                                                  |    }
                                                  |}
                                                  |""".stripMargin
        .replace("__e__", outputFileExtO.map(ext => s""""$ext"""").getOrElse("extension"))
        .replace("__f__", fname)
      // Out format is different from in format
      case Some(ToDir) => """
                            |def outFromIn(inputstr) {
                            |
                            |    return "__f__"
                            |
                            |}
                            |""".stripMargin.replace("__f__", fname)
      case _ => """
                  |def outFromIn(inputStr) {
                  |
                  |    println(">>> Having a hard time generating an output file name.")
                  |    println(">>> Is the function_type attribute filled out?")
                  |
                  |    return "output"
                  |}
                  |""".stripMargin
    }

    val setup_main_overrideInput =
      """
        |// In: Hashmap key -> DataObjects
        |// Out: Arrays of DataObjects
        |def overrideInput(params, str) {
        |
        |    // `str` in fact can be one of:
        |    // - `String`, 
        |    // - `List[String]`,
        |    // - `Map[String, String | List[String]]`
        |    // Please refer to the docs for more info
        |    def overrideArgs = params.arguments.collect{ it ->
        |      (it.value.direction == "Input" && it.value.type == "file")
        |        ? (str in List || str in HashMap)
        |            ? (str in List)
        |                ? it.value + [ "value" : str.join(it.value.multiple_sep)]
        |                : (str[it.value.name] != null)
        |                    ? (str[it.value.name] in List)
        |                        ? it.value + [ "value" : str[it.value.name].join(it.value.multiple_sep)]
        |                        : it.value + [ "value" : str[it.value.name]]
        |                    : it.value + [ "value" : "PROBLEMS" ]
        |            : it.value + [ "value" : str ]
        |        : it.value
        |    }
        |
        |    def newParams = params + [ "arguments" : overrideArgs ]
        |
        |    return newParams
        |}
        |""".stripMargin

    val setup_main_overrideOutput =
      """
        |def overrideOutput(params, str) {
        |
        |    def update = [ "value" : str ]
        |
        |    def overrideArgs = params.arguments.collect{it ->
        |      (it.direction == "Output" && it.type == "file")
        |        ? it + update
        |        : it
        |    }
        |
        |    def newParams = params + [ "arguments" : overrideArgs ]
        |
        |    return newParams
        |}
        |""".stripMargin

    /**
     * Some (implicit) conventions:
     * - `params.output/` is where the output data is published
     * - per_id is for creating directories per (sample) ID, default is true
     * - path is for modifying the layout of the output directory, default is no changes
     */
    val setup_main_process = {

      val per_idParsed:Boolean = per_id.getOrElse(true)
      val pathParsed = path.map(_.split("/").mkString("/") + "/").getOrElse("")

      // If id is the empty string, the subdirectory is not created
      val publishDirString =  per_idParsed match {
        case true => s"$${params.output}/${pathParsed}$${id}/"
        case _ => s"$${params.output}/${pathParsed}"
      }

      val publishDirStr = publish match {
        case Some(true) => s"""publishDir "$publishDirString", mode: 'copy', overwrite: true"""
        case _ => ""
      }

      val labelString = label match {
        case Some(str) => s"label '$str'"
        case _ => ""
      }

      val stageInModeStr = stageInMode match {
        case Some("copy") => "copy"
        case _ => "symlink"
      }

      val preHook = functionality.function_type match {
        case Some(ToDir) => "mkdir " + fname
        case _ => "echo Nothing before"
      }

      val outputStr = functionality.function_type match {
        case Some(ToDir) => "${output}/*"
        case _ => "${output}"
      }

      s"""
         |
         |process ${fname}_process {
         |  $labelString
         |  tag "$${id}"
         |  echo { (params.debug == true) ? true : false }
         |  cache 'deep'
         |  stageInMode "$stageInModeStr"
         |  container "$${params.dockerPrefix}$${container}"
         |  $publishDirStr
         |  input:
         |    tuple val(id), path(input), val(output), val(container), val(cli)
         |  output:
         |    tuple val("$${id}"), path("$outputStr")
         |  script:
         |    \"\"\"
         |    # Running the pre-hook when necessary
         |    $preHook
         |    # Adding NXF's `$$moduleDir` to the path in order to resolve our own wrappers
         |    export PATH="$${moduleDir}:\\$$PATH"
         |    # Echo what will be run, handy when looking at the .command.log file
         |    echo Running: $$cli
         |    # Actually run the command
         |    $$cli
         |    \"\"\"
         |
         |}
         |""".stripMargin
    }

    val outFromInStr = functionality.function_type match {
      case Some(AsIs) => "def outputFilename = getOutputFilename(updtParams)"
      case _ => "def outputFilename = outFromIn(filename)"
    }

    val setup_main_workflow =
      s"""
         |workflow $fname {
         |
         |    take:
         |    id_input_params_
         |
         |    main:
         |
         |    def key = "$fname"
         |
         |    def id_input_output_function_cli_ =
         |        id_input_params_.map{ id, input, _params ->
         |            // TODO: make sure input is List[Path], HashMap[String,Path] or Path, otherwise convert
         |            // NXF knows how to deal with an List[Path], not with HashMap !
         |            def checkedInput =
         |                (input in HashMap)
         |                    ? input.collect{ k, v -> v }.flatten()
         |                    : input
         |            // filename is either String, List[String] or HashMap[String, String]
         |            def filename =
         |                (input in List || input in HashMap)
         |                    ? (input in List)
         |                        ? input.collect{ it.name }
         |                        : input.collectEntries{ k, v -> [ k, (v in List) ? v.collect{it.name} : v.name ] }
         |                    : input.name
         |            def defaultParams = params[key] ? params[key] : [:]
         |            def overrideParams = _params[key] ? _params[key] : [:]
         |            def updtParams = defaultParams + overrideParams
         |            // now, switch to arrays instead of hashes...
         |            $outFromInStr
         |            def updtParams1 = overrideInput(updtParams, filename)
         |            def updtParams2 = overrideOutput(updtParams1, outputFilename)
         |            new Tuple5(
         |                id,
         |                checkedInput,
         |                outputFilename,
         |                updtParams2.container,
         |                renderCLI([updtParams2.command], updtParams2.arguments)
         |            )
         |        }
         |    result_ = ${fname}_process(id_input_output_function_cli_) \\
         |        | join(id_input_params_) \\
         |        | map{ id, output, input, original_params ->
         |            new Tuple3(id, output, original_params)
         |        }
         |
         |    emit:
         |    result_
         |
         |}
         |""".stripMargin

    val setup_main_entrypoint = functionality.function_type match {
      case Some(Join) => s"""
                            |workflow {
                            |
                            |   def id = params.id
                            |
                            |   def ch_ = (params.input.contains("*"))
                            |           ? Channel.fromPath(params.input)
                            |                .collect()
                            |                .map{ s -> new Tuple3(id, s, params)}
                            |           : Channel.from(Paths.get(params.input))
                            |                .map{ s -> new Tuple3(id, s, params)}
                            |
                            |   $fname(ch_)
                            |}
                            |""".stripMargin
      case _ => s"""
                   |workflow {
                   |
                   |   def id = params.id
                   |   def inputPath = Paths.get(params.input)
                   |   def ch_ = Channel.from(inputPath).map{ s -> new Tuple3(id, s, params)}
                   |
                   |   $fname(ch_)
                   |}
                   |""".stripMargin
    }

    val setup_main = PlainFile(
      name = Some("main.nf"),
      text = Some(setup_main_header +
        setup_main_utils +
        setup_main_outFromIn +
        setup_main_overrideInput +
        setup_main_overrideOutput +
        setup_main_process +
        setup_main_workflow +
        setup_main_entrypoint)
    )

    val additionalResources = mainResource match {
      case None => Nil
      case Some(_: Executable) => Nil
      case Some(_: Script) =>
        nativePlatform.modifyFunctionality(functionality).resources.getOrElse(Nil)
    }

    functionality.copy(
      resources =
        Some(additionalResources ::: List(setup_nextflowconfig, setup_main))
    )
  }
}

object NextFlowUtils {
  def quote(str: String): String = '"' + str + '"'

  def quoteLong(str: String): String = str.replace("-", "_")

  def mapToConfig(m: (String, Any), indent: String = ""): String = m match {
    case (k: String, v: List[_]) =>
      val content = v.map { pair =>
        // cast pair because type is removed due to type erasure
        mapToConfig(pair.asInstanceOf[(String, Any)], indent + "  ")
      }.mkString("\n")

      s"""$indent$k {
         |$content
         |$indent}""".stripMargin
    case (k: String, v: String) => s"""$indent$k = ${quote(v)}"""
    case (k: String, v: Boolean) => s"""$indent$k = ${v.toString}"""
    case (k: String, v: Direction) => s"""$indent$k = ${quote(v.toString)}"""
    case (k: String, v: Char) => s"""$indent$k = ${quote(v.toString)}"""
    case _ => indent + "Parsing ERROR - Not implemented yet " + m
  }

  def listMapToConfig(m: List[(String, Any)]): String = {
    m.map(mapToConfig(_)).mkString("\n")
  }

  def namespacedValueTuple(key: String, value: String)(implicit fun: Functionality): (String, String) =
    (s"${fun.name}__$key", value)

  implicit class RichDataObject[T](val dataObject: DataObject[T])(implicit fun: Functionality) {
    def valuePointer(key: String): String =
      s"$${params.${fun.name}__$key}"

    def valueOrPointer(str: String): String = {
      valuePointer(dataObject.plainName.replace("-", "_"))
    }

    def toTuple: (String, List[(String, Any)]) = {
      quoteLong(dataObject.plainName) → {
        List(
          "name" → dataObject.plainName,
          "otype" → dataObject.otype
        ) :::
          dataObject.description.map("description" → _).toList :::
          dataObject.default.map(x => "value" → valueOrPointer(x.toString)).toList :::
          List(
            "required" → dataObject.required,
            "type" → dataObject.`type`,
            "direction" → dataObject.direction,
            "multiple" → dataObject.multiple,
            "multiple_sep" -> dataObject.multiple_sep
          )
      }
    }
  }

}
