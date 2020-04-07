package com.dataintuitive.viash.targets

import com.dataintuitive.viash.functionality.{Functionality, Resource, StringObject}
import com.dataintuitive.viash.functionality.platforms.NativePlatform
import com.dataintuitive.viash.targets.environments._
import com.dataintuitive.viash.functionality.{DataObject, FileObject, Direction}
import com.dataintuitive.viash.functionality.{Input, Output}
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
  executor: Option[String],
  publishSubDir: Option[Boolean]
) extends Target {
  val `type` = "nextflow"

  val dockerTarget = DockerTarget(image, volumes, port, workdir, apt, r, python)

  def modifyFunctionality(functionality: Functionality) = {

    val dockerFunctionality = dockerTarget.modifyFunctionality(functionality)

    val resourcesPath = "/app"

    val fname = functionality.name

    def quote(str:String) = '"' + str + '"'
    def quoteLong(str:String):String = if (str.contains("-")) '"' + str + '"' else str

    // get main script/binary
    // Only the Native platform case is covered for the moment
    val mainResource = functionality.mainResource.get
    val mainPath = Paths.get(resourcesPath, mainResource.name).toFile().getPath()
    val executionCode = functionality.platform match {
      case Some(NativePlatform) => mainResource.path.getOrElse("echo No command provided")
      case _    => { println("Not implemented yet"); mainPath}
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
    def namespacedValueTuple(key: String, value: String):(String, String) =
      (s"${fname}__${key}", value)

    def valuePointer(key: String, value: String):String = s"$${params.${fname}__${key}}"


    def dataObjectToTuples[T](dataObject:DataObject[T]):List[(String, Any)] = {
      def valueOrPointer(str:String):String =
        if (! dataObject.name.contains("-"))
          valuePointer(dataObject.name, str)
        else
          // We currently have no solution for keys that contain `-`
          str

      List(
        Some(("name", dataObject.strname)),
        Some(("otype", dataObject.otype)),
        dataObject.description.map(x =>
            ("description", x.toString)),
        dataObject.default.map(x =>
            ("value", valueOrPointer(x.toString))),
        dataObject.required.map(x =>
            ("required", x)),
        Some(("type", dataObject.`type`)),
        Some(("direction", dataObject.direction))
      ).flatMap(x => x)

    }

    val namespacedParameters = functionality.arguments
      .map(dataObject => {

          val name = dataObject.strname

          println("name here: " + name)

          if (! name.contains("-")) {
            Some(
              namespacedValueTuple(
                name,
                dataObject.default.map(_.toString).getOrElse("value_not_found")
              ))
          } else {
            // We currently have no solution for keys that contain `-`
            println(s"The variable $name contains a -, removing this from the global namespace...")
            None
          }
      }).flatMap(x => x)

    println(namespacedParameters)

    val argumentsAsTuple = if (functionality.arguments.length > 0) {
      List(
        ("arguments", functionality.arguments.map(x => (quoteLong(x.name), dataObjectToTuples(x))))
      )
    } else Nil

    val extensionsAsTuple = outputFileExtO match {
      case Some(ext) => List(
        ("extensions", List(("out", ext)))
      )
      case None => Nil
    }

    val asNestedTuples:List[(String, Any)] = List(
      ("docker.enabled", true),
      ("process.container", "dataintuitive/portash"),
      ("params",
        namespacedParameters :::
        List(
          ("id", ""),
          ("outDir", "out"),
          ("input", "test.md"),
          (functionality.name,
            List(
              ("name", functionality.name),
              ("container", image),
              ("command", executionCode)
            )
            ::: extensionsAsTuple
            ::: argumentsAsTuple
            )
          )
        )
      )

    def convertBool(b: Boolean):String = if (b) "true" else "false"

    def mapToConfig(m:(String, Any), indent:String = ""):String = m match {
        case (k:String, v: List[(String, Any)]) =>
          indent + k + " {\n" + v.map(x => mapToConfig(x, indent + "  ")).mkString("\n") + "\n" + indent + "}"
        case (k:String, v: String) => indent + k + " = " + quote(v)
        case (k:String, v: Boolean) => indent + k + " = " + convertBool(v)
        case (k:String, v: Direction) => indent + k + " = " + quote(v.toString)
        case _ => indent + "Parsing ERROR - Not implemented yet " + m
    }

    def listMapToConfig(m:List[(String, Any)]) = m.map(x => mapToConfig(x)).mkString("\n")

    val setup_nextflowconfig = Resource(
      name = "nextflow.config",
      code = Some(
        listMapToConfig(asNestedTuples)
      )
    )


    val setup_main_header = s"""nextflow.preview.dsl=2
        |import java.nio.file.Paths
        |""".stripMargin('|')

    val setup_main_utils = s"""
        |// TODO: Support for short options
        |def renderCLI(command, arguments, options) {
        |
        |    def argumentsList = []
        |    def optionsList = []
        |    def argumentsMap = arguments
        |    argumentsMap.each{ it -> argumentsList << it.value }
        |    def optionsMap = options
        |    optionsMap.each{ it ->
        |        (it.type == "boolean")
        |        ? optionsList << it.otype + it.name
        |        : optionsList << it.otype + it.name + " " + it.value }
        |
        |    def command_line = command + optionsList + argumentsList
        |
        |    return command_line.join(" ")
        |}
        |""".stripMargin('|')

    /**
     * What should the output filename be, in terms of the input?
     * This is irrelevant for simple one-step function calling, but it is crucial a in a pipeline.
     * This uses the function type, but there is no check on it yet!
     * TODO: Check for conditions
     */
    val setup_main_outFromIn = functionality.ftype match {
      // in and out file format are the same
      case Some("asis") => """
          |def outFromIn(inputstr) {
          |
          |    return "${inputstr}"
          |}
          |""".stripMargin('|').replace("__e__", inputFileExtO.getOrElse("OOPS")).replace("__f__", fname)
      case Some("transform") => """
          |def outFromIn(inputstr) {
          |
          |    def splitstring = inputstr.split(/\./)
          |    def prefix = splitstring.head()
          |    def extension = splitstring.last()
          |
          |    return prefix + "." + "__f__" + "." + "__e__"
          |}
          |""".stripMargin('|').replace("__e__", inputFileExtO.getOrElse("OOPS")).replace("__f__", fname)
      // Out format is different from in format
      case Some("convert") => """
          |def outFromIn(inputstr) {
          |
          |    def splitstring = inputstr.split(/\./)
          |    def prefix = splitstring.head()
          |    def extension = splitstring.last()
          |
          |    return prefix + "." + "__f__" + "." + "__e__"
          |}
          |""".stripMargin('|').replace("__e__", outputFileExtO.getOrElse("OOPS")).replace("__f__", fname)
      // Out format is different from in format
      case Some("unzip") => """
          |def outFromIn(inputstr) {
          |
          |    def splitstring = inputstr.split(/\./)
          |    def newStr = splitstring.dropRight(1)
          |
          |    return newStr.join(".")
          |}
          |""".stripMargin('|')
      // Out format is different from in format
      case Some("todir") => """
          |def outFromIn(inputstr) {
          |
          |    return "__f__"
          |
          |}
          |""".stripMargin('|').replace("__f__", fname)
      // Out format is different from in format
      case Some("join") => """
          |def outFromIn(input) {
          |    if (input.getClass() == sun.nio.fs.UnixPath) {
          |        def splitString = input.name.split(/\./)
          |        def prefix = splitString.head()
          |        def extension = splitString.last()
          |
          |        return prefix + "." + "__f__" + "." + "__e__"
          |    } else {
          |        // We're in join mode
          |        return "__f__" + "." + "__e__"
          |    }
          |}
          |""".stripMargin('|').replace("__e__", outputFileExtO.getOrElse("OOPS")).replace("__f__", fname)
      case _ => """
          |def outFromIn(inputStr) {
          |
          |    return "I-have-no-idea-what-this-file-should-be"
          |}
          |""".stripMargin('|')
    }

    val setup_main_overrideInput = """
        |// In: Hashmap key -> DataqObjects
        |// Out: Arrays of DataObjects
        |def overrideInput(params, str) {
        |
        |    def overrideOptions = []
        |    def overrideArgs = []
        |
        |    def update = [ "value" : str ]
        |
        |    params.arguments.each{ it ->
        |      (it.value.direction == "Input" && it.value.type == "file")
        |        ? overrideArgs << it.value + update
        |        : overrideArgs << it.value
        |    }
        |    params.options.each{ it ->
        |      (it.value.direction == "Input" && it.value.type == "file")
        |        ? overrideOptions << it.value + update
        |        : overrideOptions << it.value
        |    }
        |
        |    def newParams = params + [ "options" : overrideOptions] + [ "arguments" : overrideArgs ]
        |
        |    return newParams
        |}
        |""".stripMargin('|')

    val setup_main_overrideOutput = """
        |def overrideOutput(params, str) {
        |
        |    def overrideOptions = []
        |    def overrideArgs = []
        |
        |    def update = [ "value" : str ]
        |
        |    params.arguments.each{it ->
        |      (it.direction == "Output" && it.type == "file")
        |        ? overrideArgs << it + update
        |        : overrideArgs << it
        |    }
        |    params.options.each{ it ->
        |     (it.direction == "Output" && it.type == "file")
        |        ? overrideOptions << it + update
        |        : overrideOptions << it
        |    }
        |
        |    def newParams = params + [ "options" : overrideOptions] + [ "arguments" : overrideArgs ]
        |
        |    return newParams
        |}
        |""".stripMargin('|')

    /**
     * Some (implicit) conventions:
     * - `outDir/` is where the output data is published
     * - For multiple samples, an additional subdir `id` can be created, but blank by default
     * - A boolean option `publishSubdir` is available to store processing steps in subdirs
     */
    val setup_main_process = {

      val publishDirString = publishSubDir match {
        case Some(true) => "${params.outDir}/${id}/" + fname
        case _ => "${params.outDir}/${id}"
      }

      val stageInMode = functionality.ftype match {
        case Some("unzip") => "copy"
        case _ => "symlink"
      }

      val preHook = functionality.ftype match {
        case Some("todir") => "mkdir " + fname
        case _ => "echo Nothing before"
      }

      s"""
        |
        |process simpleBashExecutor {
        |  echo { (params.debug == true) ? true : false }
        |  cache 'deep'
        |  stageInMode "$stageInMode"
        |  container "$${container}"
        |  // If id is the empty string, the subdirectory is not created
        |  publishDir "$publishDirString", mode: 'copy', overwrite: true
        |  input:
        |    tuple val(id), path(input), val(output), val(container), val(cli)
        |  output:
        |    tuple val("$${id}"), path("$${output}")
        |  script:
        |    \"\"\"
        |    $preHook
        |    echo Running: $$cli
        |    $$cli
        |    \"\"\"
        |
        |}
        |""".stripMargin('|')
    }

    val setup_main_workflow = functionality.ftype match {
      case Some("join") => s"""
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
        |            // If the input is not a path name, it's probably an array
        |            // that needs to be concatenated.
        |            def filename = (input.getClass() == sun.nio.fs.UnixPath)
        |                            ? input.name
        |                            : input.join(' ')
        |            def inputPath = (input.getClass() == sun.nio.fs.UnixPath)
        |                              ? input
        |                              : Paths.get(input.join(' '))
        |            def outputFilename = outFromIn(input)
        |            def defaultParams = params[key] ? params[key] : [:]
        |            def overrideParams = _params[key] ? _params[key] : [:]
        |            def updtParams = defaultParams + overrideParams
        |            // now, switch to arrays instead of hashes...
        |            def updtParams1 = overrideInput(updtParams, filename)
        |            def updtParams2 = overrideOutput(updtParams1, outputFilename)
        |            new Tuple5(
        |                id,
        |                inputPath,
        |                outputFilename,
        |                updtParams2.container,
        |                renderCLI([updtParams2.command], updtParams2.arguments, updtParams2.options)
        |            )
        |        }
        |
        |    result_ = simpleBashExecutor(id_input_output_function_cli_) \\
        |        | join(id_input_params_) \\
        |        | map{ id, output, input, original_params ->
        |            new Tuple3(id, output, original_params)
        |        }
        |
        |    emit:
        |    result_
        |
        |}
        |""".stripMargin('|')
      case _ => s"""
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
        |            def filename = ""
        |            def outputFilename = ""
        |            if (input.getClass() == sun.nio.fs.UnixPath) {
        |                // Just a file path as input
        |                filename = input.name
        |                outputFilename = outFromIn(input.name)
        |            } else if (input.getClass() == java.lang.String) {
        |                // Expect this to be a path, so convert to one
        |                filename = Paths.get(input)
        |                outputFilename = outFromIn(input)
        |            } else {
        |                // Probably join mode, act accordingly
        |                filename = input.map{it.toString()}.join(' ')
        |                ouputFilename = outFromInt(input)
        |            }
        |            def defaultParams = params[key] ? params[key] : [:]
        |            def overrideParams = _params[key] ? _params[key] : [:]
        |            def updtParams = defaultParams + overrideParams
        |            // now, switch to arrays instead of hashes...
        |            def updtParams1 = overrideInput(updtParams, filename)
        |            def updtParams2 = overrideOutput(updtParams1, outputFilename)
        |            new Tuple5(
        |                id,
        |                input,
        |                outputFilename,
        |                updtParams2.container,
        |                renderCLI([updtParams2.command], updtParams2.arguments, updtParams2.options)
        |            )
        |        }
        |
        |    result_ = simpleBashExecutor(id_input_output_function_cli_) \\
        |        | join(id_input_params_) \\
        |        | map{ id, output, input, original_params ->
        |            new Tuple3(id, output, original_params)
        |        }
        |
        |    emit:
        |    result_
        |
        |}
        |""".stripMargin('|')
    }

    val setup_main_entrypoint = functionality.ftype match {
      case Some("join") => """
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
        |   md_concat(ch_)
        |}
        |""".stripMargin('|')
      case _ => s"""
        |workflow {
        |
        |   def id = params.id
        |   def inputPath = Paths.get(params.input)
        |   def ch_ = Channel.from(inputPath).map{ s -> new Tuple3(id, s, params)}
        |
        |   $fname(ch_)
        |}
        |""".stripMargin('|')
    }

    val setup_main = Resource(
      name = "main.nf",
      code = Some(setup_main_header +
                  setup_main_utils +
                  setup_main_outFromIn +
                  setup_main_overrideInput +
                  setup_main_overrideOutput +
                  setup_main_process +
                  setup_main_workflow +
                  setup_main_entrypoint)
    )

    dockerFunctionality.copy(
        resources =
          dockerFunctionality.resources ::: List(setup_nextflowconfig, setup_main)
    )
  }

}
