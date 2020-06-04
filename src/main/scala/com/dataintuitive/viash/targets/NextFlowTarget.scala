package com.dataintuitive.viash.targets

import com.dataintuitive.viash.functionality._
import com.dataintuitive.viash.functionality.resources._
import com.dataintuitive.viash.functionality.dataobjects._
import com.dataintuitive.viash.targets.environments._
import java.nio.file.Paths

/**
 * Target class for generating NextFlow (DSL2) modules.
 */
case class NextFlowTarget(
  image: String,
  apt: Option[AptEnvironment] = None,
  r: Option[REnvironment] = None,
  python: Option[PythonEnvironment] = None,
  executor: Option[String],
  publish: Option[Boolean],
  publishSubDir: Option[Boolean],
  label: Option[String],
  stageInMode: Option[String]
) extends Target {
  val `type` = "nextflow"

  val nativeTarget = NativeTarget(r, python)

  def modifyFunctionality(functionality: Functionality, test: Option[Script]) = {
    import NextFlowUtils._
    implicit val fun = functionality

    val resourcesPath = "/app"
    val fname = functionality.name

    // get main script/binary
    val mainResource = functionality.mainScript
    val mainPath = Paths.get(resourcesPath, mainResource.get.filename).toFile().getPath()
    val executionCode = mainResource match {
      case Some(e: Executable) => e.path.get
      case _ => fname
    }

    val allPars = functionality.arguments

    def inputFileExtO = allPars
            .filter(_.`type` == "file")
            .filter(_.direction == Input)
            .headOption
            .flatMap(_.default.map(_.toString.split('.').last))

    def outputFileExtO = allPars
            .filter(_.`type` == "file")
            .filter(_.direction == Output)
            .headOption
            .flatMap(_.default.map(_.toString.split('.').last))

    /**
     * All values for arguments/parameters are defined in the root of
     * the params structure. the function name is prefixed as a namespace
     * identifier. A "__" is used to seperate namespace and arg/option.
     */
    // TODO: find a solution of the options containg a `-`
    val namespacedParameters =
      functionality.arguments.flatMap(dataObject => {
        val name = dataObject.plainName

        if (!name.contains("-")) {
          Some(
            namespacedValueTuple(
              name,
              dataObject.default.map(_.toString).getOrElse("value_not_found")
            )
          )
        } else {
          // We currently have no solution for keys that contain `-`
          println(s"The variable $name contains a -, removing this from the global namespace...")
          None
        }
      })

    val argumentsAsTuple =
      if (functionality.arguments.length > 0) {
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

    val asNestedTuples: List[(String, Any)] = List(
      "docker.enabled" → true,
      "process.container" → "dataintuitive/portash",
      "params" → {
        namespacedParameters :::
        List(
          "id" → "",
          "outDir" → "out",
          "input" → "test.md",
          functionality.name → {
            List(
              "name" → functionality.name,
              "container" → image,
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

    val setup_main_header = s"""nextflow.preview.dsl=2
        |import java.nio.file.Paths
        |""".stripMargin

    val setup_main_utils = s"""
        |def renderCLI(command, arguments) {
        |
        |    def argumentsList = arguments.collect{ it ->
        |        (it.otype == "")
        |            ? it.value
        |            : (it.type == "boolean_true")
        |                ? it.otype + it.name
        |                : it.otype + it.name + " " + it.value
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
          |def outFromIn(inputstr) {
          |
          |    return "${inputstr}"
          |}
          |""".stripMargin.replace("__e__", inputFileExtO.getOrElse("OOPS")).replace("__f__", fname)
      // Out format is different from in format
      case Some(Convert) => """
          |def outFromIn(inputstr) {
          |
          |    def splitstring = inputstr.split(/\./)
          |    def prefix = splitstring.head()
          |    def extension = splitstring.last()
          |
          |    return prefix + "." + "__f__" + "." + "__e__"
          |}
          |""".stripMargin.replace("__e__", outputFileExtO.getOrElse("OOPS")).replace("__f__", fname)
      // Out format is different from in format
      case Some(ToDir) => """
          |def outFromIn(inputstr) {
          |
          |    return "__f__"
          |
          |}
          |""".stripMargin.replace("__f__", fname)
      // Out format is different from in format
      case Some(Join) => """
          |// files is either String or List[String]
          |def outFromIn(files) {
          |    if (files in List) {
          |        // We're in join mode, files is List[String]
          |        return "concat" + "." + "md"
          |    } else {
          |        // files filename is just a String
          |        def splitString = files.split(/\./)
          |        def prefix = splitString.head()
          |        def extension = splitString.last()
          |        return prefix + "." + "concat" + "." + "md"
          |    }
          |}
          |""".stripMargin.replace("__e__", outputFileExtO.getOrElse("OOPS")).replace("__f__", fname)
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

    val setup_main_overrideInput = """
        |// In: Hashmap key -> DataObjects
        |// Out: Arrays of DataObjects
        |def overrideInput(params, str) {
        |
        |    // In 'join' mode, concatenate the strings and add double quotes around the values
        |    def update = (str in List)
        |        ? [ "value" : str.join(" ") ]
        |        : [ "value" : str ]
        |
        |    def overrideArgs = params.arguments.collect{ it ->
        |      (it.value.direction == "Input" && it.value.type == "file")
        |        ? it.value + update
        |        : it.value
        |    }
        |
        |    def newParams = params + [ "arguments" : overrideArgs ]
        |
        |    return newParams
        |}
        |""".stripMargin

    val setup_main_overrideOutput = """
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
     * - `outDir/` is where the output data is published
     * - For multiple samples, an additional subdir `id` can be created, but blank by default
     * - A boolean option `publishSubdir` is available to store processing steps in subdirs
     */
    val setup_main_process = {

      // If id is the empty string, the subdirectory is not created
      val publishDirString = publishSubDir match {
        case Some(true) => "${params.outDir}/${id}/" + fname
        case _ => "${params.outDir}/${id}"
      }

      val publishDirStr = publish match {
        case Some(false) => ""
        case _ => s"""publishDir "$publishDirString", mode: 'copy', overwrite: true"""
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
        |process executor {
        |  $labelString
        |  tag "$${id}"
        |  echo { (params.debug == true) ? true : false }
        |  cache 'deep'
        |  stageInMode "$stageInModeStr"
        |  container "$${container}"
        |  $publishDirStr
        |  input:
        |    tuple val(id), path(input), val(output), val(container), val(cli)
        |  output:
        |    tuple val("$${id}"), path("${outputStr}")
        |  script:
        |    \"\"\"
        |    # Running the pre-hook when necessary
        |    $preHook
        |    # Adding NXF's `$$moduleDir` to the path in order to resolve our own wrappers
        |    export PATH="$${moduleDir}:$$PATH"
        |    # Echo what will be run, handy when looking at the .command.log file
        |    echo Running: $$cli
        |    # Actually run the command
        |    $$cli
        |    \"\"\"
        |
        |}
        |""".stripMargin
    }

    val setup_main_workflow = s"""
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
        |            // TODO: make sure input is List[Path] or Path, otherwise convert
        |            def checkedInput = input
        |            // filename is either String or List[String]
        |            def filename =
        |                (checkedInput in List)
        |                    ? checkedInput.collect{ it.name }
        |                    : checkedInput.name
        |            // NXF knows how to deal with an List[Path]
        |            def outputFilename = outFromIn(filename)
        |            def defaultParams = params[key] ? params[key] : [:]
        |            def overrideParams = _params[key] ? _params[key] : [:]
        |            def updtParams = defaultParams + overrideParams
        |            // now, switch to arrays instead of hashes...
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
        |
        |    result_ = executor(id_input_output_function_cli_) \\
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
      case None => {
        println("No additional resources required")
        Nil
      }
      case Some(e: Executable) => {
        println("No additional resources required")
        Nil
      }
      case Some(e: Script) => {
        println(s"Add ${e.`type`} resources")
        nativeTarget.modifyFunctionality(functionality, None).resources
      }
    }

    functionality.copy(
        resources =
          additionalResources ::: List(setup_nextflowconfig, setup_main)
    )
  }
}

object NextFlowUtils {
  def quote(str: String) = '"' + str + '"'

  def quoteLong(str: String) = if (str.contains("-")) '"' + str + '"' else str

  def mapToConfig(m: (String, Any), indent: String = ""): String = m match {
    case (k: String, v: List[_]) => {
      val content = v.map { pair =>
        // cast pair because type is removed due to type erasure
        mapToConfig(pair.asInstanceOf[(String, Any)], indent + "  ")
      }.mkString("\n")

      s"""$indent$k {
        |$content
        |$indent}""".stripMargin
    }
    case (k: String, v: String) => s"""$indent$k = ${quote(v)}"""
    case (k: String, v: Boolean) => s"""$indent$k = ${v.toString}"""
    case (k: String, v: Direction) => s"""$indent$k = ${quote(v.toString)}"""
    case _ => indent + "Parsing ERROR - Not implemented yet " + m
  }

  def listMapToConfig(m: List[(String, Any)]) = {
    m.map(mapToConfig(_)).mkString("\n")
  }

  def namespacedValueTuple(key: String, value: String)(implicit fun: Functionality): (String, String) =
    (s"${fun.name}__${key}", value)

  implicit class RichDataObject[T](val dataObject: DataObject[T])(implicit fun: Functionality) {
    def valuePointer(key: String, value: String): String =
      s"$${params.${fun.name}__${key}}"

    def valueOrPointer(str: String): String = {
      if (!dataObject.plainName.contains("-")) {
        valuePointer(dataObject.plainName, str)
      } else {
        // We currently have no solution for keys that contain `-`
        str
      }
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
          "direction" → dataObject.direction
        )
      }
    }
  }
}