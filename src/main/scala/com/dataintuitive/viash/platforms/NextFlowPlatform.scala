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

    val namespacedParameters:List[ConfigTuple] =
      functionality.arguments.map { dataObject =>
        namespacedValueTuple(
          dataObject.plainName.replace("-", "_"),
          dataObject.default.map(_.toString).getOrElse("value_not_found")
        )(fun)
      }

    val argumentsAsTuple:List[ConfigTuple] =
      if (functionality.arguments.nonEmpty) {
        List(
          "arguments" → NestedValue(functionality.arguments.map(dataObjectToConfigTuple(_)))
        )
      } else {
        Nil
      }

    val extensionsAsTuple:List[ConfigTuple] = outputFileExtO match {
      case Some(ext) => List(
        "extensions" → NestedValue(List("out" -> ext))
      )
      case None => Nil
    }

    val containerName:String = {
      val autogen = functionality.namespace.map( _ + "/" + functionality.name).getOrElse(functionality.name)
      image.getOrElse(autogen)
    }

    val containerTag:String = version.map(_.toString).getOrElse("latest")

    val mainParams:List[ConfigTuple] = List(
        "name" → functionality.name,
        "container" → containerName,
        "containerTag" -> containerTag,
        "command" → executionCode
    )

    // fetch test information
    val tests = functionality.tests.getOrElse(Nil)
    val testPaths = tests.map(test => test.path.getOrElse("/dev/null")).toList
    val testScript:List[String] =
        tests.filter(_.isInstanceOf[Script]).map{
          case test: Script => test.filename
        }

    // If no tests are defined, isDefined is set to FALSE
    val testConfig:List[ConfigTuple] = List("tests" -> NestedValue(
        List(
          tupleToConfigTuple("isDefined" -> (tests.size > 0)),
          tupleToConfigTuple("testScript" -> testScript.headOption.getOrElse("NA")),
          tupleToConfigTuple("testResources" -> testPaths)
        )
      ))

    /**
     * A few notes:
     * 1. input and output are initialized as empty strings, so that no warnings appear.
     * 2. id is initialized as empty string, which makes sense in test scenarios.
     */
    val asNestedTuples: List[ConfigTuple] = List(
      "docker.enabled" → true,
      "process.container" → "dataintuitive/portash",
      "params" → NestedValue(
        namespacedParameters :::
        List(
          tupleToConfigTuple("id" → ""),
          tupleToConfigTuple("dockerPrefix" -> ""),
          tupleToConfigTuple("input" → ""),
          tupleToConfigTuple("output" → ""),
          tupleToConfigTuple(functionality.name → NestedValue(
            mainParams :::
            testConfig :::
            extensionsAsTuple :::
            argumentsAsTuple
          ))
        )
    ))

    val setup_nextflowconfig = PlainFile(
      dest = Some("nextflow.config"),
      text = Some(listMapToConfig(asNestedTuples))
    )

    val setup_main_header =
      s"""nextflow.preview.dsl=2
         |import java.nio.file.Paths
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
         |
         |def effectiveContainer(processParams) {
         |    def _registry = params.containsKey("containerRegistry") ? params.containerRegistry + "/" : ""
         |    def _name = processParams.container
         |    def _tag = params.containsKey("containerTag") ? "$${params.containerTag}" : "$${processParams.containerTag}"
         |
         |    return "$${_registry}$${_name}:$${_tag}"
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
        |                    : it.value
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
        case Some(true) => s"""publishDir "$publishDirString", mode: 'copy', overwrite: true, enabled: !params.test"""
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
        case Some(ToDir) => "${output}"
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
         |  container "$${container}"
         |  $publishDirStr
         |  input:
         |    tuple val(id), path(input), val(output), val(container), val(cli)
         |  output:
         |    tuple val("$${id}"), path("$outputStr")
         |  script:
         |    if (params.test)
         |        \"\"\"
         |        # Some useful stuff
         |        export NUMBA_CACHE_DIR=/tmp/numba-cache
         |        # Running the pre-hook when necessary
         |        $preHook
         |        # Adding NXF's `$$moduleDir` to the path in order to resolve our own wrappers
         |        export PATH="./:$${moduleDir}:\\$$PATH"
         |        ./$${params.${fname}.tests.testScript} | tee $$output
         |        \"\"\"
         |    else
         |        \"\"\"
         |        # Some useful stuff
         |        export NUMBA_CACHE_DIR=/tmp/numba-cache
         |        # Running the pre-hook when necessary
         |        $preHook
         |        # Adding NXF's `$$moduleDir` to the path in order to resolve our own wrappers
         |        export PATH="$${moduleDir}:\\$$PATH"
         |        $$cli
         |        \"\"\"
         |}
         |""".stripMargin
    }

    val outFromInStr = functionality.function_type match {
      case Some(AsIs) => "def outputFilename = getOutputFilename(updtParams)"
      case _ => "def outputFilename = (!params.test) ? outFromIn(filename) : updtParams.output"
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
         |                effectiveContainer(updtParams2),
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

    val setup_test_entrypoint = s"""
                  |workflow test {
                  |
                  |   take:
                  |   rootDir
                  |
                  |   main:
                  |   params.test = true
                  |   params.${fname}.output = "${fname}.log"
                  |
                  |   Channel.from(rootDir) \\
                  |        | view \\
                  |        | filter { params.${fname}.tests.isDefined } \\
                  |        | map{ p -> new Tuple3(
                  |                    "tests",
                  |                    params.${fname}.tests.testResources.collect{ file( p + it ) },
                  |                    params
                  |                )} \\
                  |        | ${fname}
                  |
                  |    emit:
                  |    ${fname}.out
                  |}
                  """.stripMargin

    val setup_main = PlainFile(
      dest = Some("main.nf"),
      text = Some(setup_main_header +
        setup_main_utils +
        setup_main_outFromIn +
        setup_main_overrideInput +
        setup_main_overrideOutput +
        setup_main_process +
        setup_main_workflow +
        setup_main_entrypoint +
        setup_test_entrypoint)
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

  import scala.reflect.runtime.universe._

  def quote(str: String): String = '"' + str + '"'

  def quoteLong(str: String): String = str.replace("-", "_")

  abstract trait ValueType

  case class PlainValue[A:TypeTag](val v:A) extends ValueType {
    def toConfig:String = v match {
      case s: String if typeOf[String] =:= typeOf[A] =>
        s"""${quote(s)}"""
      case b: Boolean if typeOf[Boolean] =:= typeOf[A] =>
        s"""${b.toString}"""
      case c: Char if typeOf[Char] =:= typeOf[A]  =>
        s"""${quote(c.toString)}"""
      case i: Int if typeOf[Int] =:= typeOf[A]  =>
        s"""${i}"""
      case l: List[_] =>
        l.map(el => quote(el.toString)).mkString("[ ", ", ", " ]")
      case _ =>
        "Parsing ERROR - Not implemented yet " + v
    }
  }

  case class ConfigTuple(val tuple: (String,ValueType)) {
    def toConfig(indent: String = "  "):String = {
      val (k,v) = tuple
      v match {
        case pv: PlainValue[_] =>
          s"""$indent$k = ${pv.toConfig}"""
        case NestedValue(nv) =>
          nv.map(_.toConfig(indent + "  ")).mkString(s"$indent$k {\n", "\n", s"\n$indent}")
      }
    }
  }

  case class NestedValue(val v:List[ConfigTuple]) extends ValueType

  implicit def tupleToConfigTuple[A:TypeTag](tuple: (String, A)):ConfigTuple = {
    val (k,v) = tuple
    v match {
      case NestedValue(nv) => new ConfigTuple((k, NestedValue(nv)))
      case _ => new ConfigTuple((k, PlainValue(v)))
    }
  }

  def listMapToConfig(m: List[ConfigTuple]): String = {
    m.map(_.toConfig()).mkString("\n")
  }

  def namespacedValueTuple(key: String, value: String)(implicit fun: Functionality): ConfigTuple =
    (s"${fun.name}__$key", value)

  implicit def dataObjectToConfigTuple[T:TypeTag](dataObject: DataObject[T])(implicit fun: Functionality):ConfigTuple = {

    def valuePointer(key: String): String =
      s"$${params.${fun.name}__$key}"

    def valueOrPointer(str: String): String = {
      valuePointer(dataObject.plainName.replace("-", "_"))
    }

    quoteLong(dataObject.plainName) → NestedValue(
      tupleToConfigTuple("name" → dataObject.plainName) ::
      tupleToConfigTuple("otype" → dataObject.otype) ::
      tupleToConfigTuple("required" → dataObject.required) ::
      tupleToConfigTuple("type" → dataObject.`type`) ::
      tupleToConfigTuple("direction" → dataObject.direction.toString) ::
      tupleToConfigTuple("multiple" → dataObject.multiple) ::
      tupleToConfigTuple("multiple_sep" -> dataObject.multiple_sep) ::
      dataObject.description
        .map(x => List(tupleToConfigTuple("description" → x.toString))).getOrElse(Nil) :::
      dataObject.default
        .map(x => List(tupleToConfigTuple("value" → valueOrPointer(x.toString)))).getOrElse(Nil)
      )
  }
}

// vim: tabstop=2:softtabstop=2:shiftwidth=2:expandtab
