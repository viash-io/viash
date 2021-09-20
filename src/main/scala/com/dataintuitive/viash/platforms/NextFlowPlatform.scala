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

package com.dataintuitive.viash.platforms

import com.dataintuitive.viash.functionality._
import com.dataintuitive.viash.functionality.resources._
import com.dataintuitive.viash.functionality.dataobjects._
import com.dataintuitive.viash.config.Version
import com.dataintuitive.viash.helpers.{Docker, Bash}

/**
 * / * Platform class for generating NextFlow (DSL2) modules.
 */
case class NextFlowPlatform(
  id: String = "nextflow",
  image: Option[String],
  tag: Option[Version] = None,
  version: Option[Version] = None,
  registry: Option[String] = None,
  namespace_separator: String = "_",
  executor: Option[String] = None,
  publish: Option[Boolean] = None,
  per_id: Option[Boolean] = None,
  separate_multiple_outputs: Boolean = true,
  path: Option[String] = None,
  label: Option[String] = None,
  labels: List[String] = Nil,
  stageInMode: Option[String] = None,
  directive_cpus: Option[Integer] = None,
  directive_max_forks: Option[Integer] = None,
  directive_time: Option[String] = None,
  directive_memory: Option[String] = None,
  directive_cache: Option[String] = None,
  oType: String = "nextflow"
) extends Platform {
  assert(version.isEmpty, "nextflow platform: attribute 'version' is deprecated")

  private val nativePlatform = NativePlatform(id = id)

  def modifyFunctionality(functionality: Functionality): Functionality = {
    import NextFlowUtils._
    implicit val fun: Functionality = functionality

    val fname = functionality.name

    // get image info
    val imageInfo = Docker.getImageInfo(
      functionality = Some(functionality),
      registry = registry,
      name = image,
      tag = tag.map(_.toString),
      namespaceSeparator = namespace_separator
    )

    // get main script/binary
    val mainResource = functionality.mainScript
    val executionCode = mainResource match {
      case Some(e: Executable) => e.path.get
      case _ => fname
    }

    val allPars = functionality.arguments

    val outputs = allPars
      .filter(_.isInstanceOf[FileObject])
      .count(_.direction == Output)

    // All values for arguments/parameters are defined in the root of
    // the params structure. the function name is prefixed as a namespace
    // identifier. A "__" is used to separate namespace and arg/option.
    //
    // Required arguments also get a params.<argument> entry so that they can be
    // called using --param value when using those standalone.

    /**
      * A representation of viash's functionality.arguments is converted into a
      * nextflow.config data structure under params.<function>.arguments.
      *
      * A `value` attribute is added that points to params.<function>__<argument>.
      * In turn, a pointer is configured to params.argument.
      *
      * The goal is to have a configuration file that works both when running
      * the module standalone as well as in a pipeline.
      */
    val namespacedParameters: List[ConfigTuple] = {
      functionality.arguments.flatMap { dataObject => (dataObject.required, dataObject.default) match {
        case (true, _) =>
          println(s"> ${dataObject.plainName} in $fname is set to be required.")
          println(s"> --${dataObject.plainName} <...> has to be specified when running this module standalone.")
          Some(
            namespacedValueTuple(
              dataObject.plainName.replace("-", "_"),
              "viash_no_value"
            )(fun)
          )
        case (false, Some(x)) =>
          Some(
            namespacedValueTuple(
              dataObject.plainName.replace("-", "_"),
              Bash.escape(x.toString, backtick = false, newline = true, quote = true)
            )(fun)
          )
        case (false, None) =>
          Some(
            namespacedValueTuple(
              dataObject.plainName.replace("-", "_"),
              "no_default_value_configured"
            )(fun)
          )
      }}
    }

    val argumentsAsTuple: List[ConfigTuple] =
      if (functionality.arguments.nonEmpty) {
        List(
          "arguments" → NestedValue(functionality.arguments.map(dataObjectToConfigTuple(_)))
        )
      } else {
        Nil
      }

    val mainParams: List[ConfigTuple] = List(
      "name" → functionality.name,
      "container" → imageInfo.name,
      "containerTag" -> imageInfo.tag,
      "containerRegistry" -> imageInfo.registry.getOrElse(""),
      "command" → executionCode
    )

    // fetch test information
    val tests = functionality.tests.getOrElse(Nil)
    val testPaths = tests.map(test => test.path.getOrElse("/dev/null"))
    val testScript: List[String] = {
        tests.flatMap{
          case test: Script => Some(test.filename)
          case _ => None
        }
    }

            // If no tests are defined, isDefined is set to FALSE
    val testConfig: List[ConfigTuple] = List("tests" -> NestedValue(
        List(
          tupleToConfigTuple("isDefined" -> tests.nonEmpty),
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
      "def viash_temp = System.getenv(\"VIASH_TEMP\") ?: \"/tmp/\"\n  docker.runOptions" → "-i -v ${baseDir}:${baseDir} -v $viash_temp:$viash_temp",
      "process.container" → "dataintuitive/viash",
      "params" → NestedValue(
        namespacedParameters :::
        List(
          tupleToConfigTuple("id" → ""),
          tupleToConfigTuple("testScript" -> testScript.headOption.getOrElse("")), // TODO: what about when there are multiple tests?
          tupleToConfigTuple("testResources" -> testPaths),
          tupleToConfigTuple(functionality.name → NestedValue(
            mainParams :::
            testConfig :::
            argumentsAsTuple
          ))
        )
    ))

    val setup_nextflowconfig = PlainFile(
      dest = Some("nextflow.config"),
      text = Some(listMapToConfig(asNestedTuples))
    )

    val setup_main_header =
      s"""nextflow.enable.dsl=2
        |
        |params.test = false
        |params.debug = false
        |params.publishDir = "./"
        |""".stripMargin

    val setup_main_outputFilters: String = {
      if (separate_multiple_outputs) {
        allPars
          .filter(_.isInstanceOf[FileObject])
          .filter(_.direction == Output)
          .map(par =>
            s"""
               |// A process that filters out ${par.plainName} from the output Map
               |process filter${par.plainName.capitalize} {
               |
               |  input:
               |    tuple val(id), val(input), val(_params)
               |  output:
               |    tuple val(id), val(output), val(_params)
               |  when:
               |    input.keySet().contains("${par.plainName}")
               |  exec:
               |    output = input["${par.plainName}"]
               |
               |}""".stripMargin
          ).mkString("\n")
      } else {
        ""
      }
    }

    val setup_main_check =
      s"""
        |// A function to verify (at runtime) if all required arguments are effectively provided.
        |def checkParams(_params) {
        |  _params.arguments.collect{
        |    if (it.value == "viash_no_value") {
        |      println("[ERROR] option --$${it.name} not specified in component $fname")
        |      println("exiting now...")
        |        exit 1
        |    }
        |  }
        |}
        |
        |""".stripMargin


    val setup_main_utils =
      s"""
        |def escape(str) {
        |  return str.replaceAll('\\\\\\\\', '\\\\\\\\\\\\\\\\').replaceAll("\\"", "\\\\\\\\\\"").replaceAll("\\n", "\\\\\\\\n").replaceAll("`", "\\\\\\\\`")
        |}
        |
        |def renderArg(it) {
        |  if (it.otype == "") {
        |    return "\'" + escape(it.value) + "\'"
        |  } else if (it.type == "boolean_true") {
        |    if (it.value.toLowerCase() == "true") {
        |      return it.otype + it.name
        |    } else {
        |      return ""
        |    }
        |  } else if (it.type == "boolean_false") {
        |    if (it.value.toLowerCase() == "true") {
        |      return ""
        |    } else {
        |      return it.otype + it.name
        |    }
        |  } else if (it.value == "no_default_value_configured") {
        |    return ""
        |  } else {
        |    def retVal = it.value in List && it.multiple ? it.value.join(it.multiple_sep): it.value
        |    return it.otype + it.name + " \'" + escape(retVal) + "\'"
        |  }
        |}
        |
        |def renderCLI(command, arguments) {
        |  def argumentsList = arguments.collect{renderArg(it)}.findAll{it != ""}
        |
        |  def command_line = command + argumentsList
        |
        |  return command_line.join(" ")
        |}
        |
        |def effectiveContainer(processParams) {
        |  def _registry = params.containsKey("containerRegistry") ? params.containerRegistry : processParams.containerRegistry
        |  def _name = processParams.container
        |  def _tag = params.containsKey("containerTag") ? params.containerTag : processParams.containerTag
        |
        |  return (_registry == "" ? "" : _registry + "/") + _name + ":" + _tag
        |}
        |
        |// Convert the nextflow.config arguments list to a List instead of a LinkedHashMap
        |// The rest of this main.nf script uses the Map form
        |def argumentsAsList(_params) {
        |  def overrideArgs = _params.arguments.collect{ key, value -> value }
        |  def newParams = _params + [ "arguments" : overrideArgs ]
        |  return newParams
        |}
        |
        |""".stripMargin

    val setup_main_outFromIn = functionality.function_type match {
      // Out format is different from in format
      case Some(Convert) | Some(Join) | Some(ToDir) | None =>
        s"""
          |// Use the params map, create a hashmap of the filenames for output
          |// output filename is <sample>.<method>.<arg_name>[.extension]
          |def outFromIn(_params) {
          |
          |  def id = _params.id
          |
          |  _params
          |    .arguments
          |    .findAll{ it -> it.type == "file" && it.direction == "Output" }
          |    .collect{ it ->
          |      // If an 'example' attribute is present, strip the extension from the filename,
          |      // If a 'dflt' attribute is present, strip the extension from the filename,
          |      // Otherwise just use the option name as an extension.
          |      def extOrName =
          |        (it.example != null)
          |          ? it.example.split(/\\./).last()
          |          : (it.dflt != null)
          |            ? it.dflt.split(/\\./).last()
          |            : it.name
          |      // The output filename is <sample> . <modulename> . <extension>
          |      // Unless the output argument is explicitly specified on the CLI
          |      def newValue =
          |        (it.value == "viash_no_value")
          |          ? "$fname." + it.name + "." + extOrName
          |          : it.value
          |      def newName =
          |        (id != "")
          |          ? id + "." + newValue
          |          : it.name + newValue
          |      it + [ value : newName ]
          |    }
          |
          |}
          |""".stripMargin
      case Some(AsIs) =>
        """
          |// Only perform a selection on the appropriate output arguments of type `file`.
          |def outFromIn(_params) {
          |
          |  def id = _params.id
          |
          |  _params
          |    .arguments
          |    .findAll{ it -> it.type == "file" && it.direction == "Output" }
          |
          |}
          |""".stripMargin
      case _ =>
        """
          |def outFromIn(inputStr) {
          |
          |  println(">>> Having a hard time generating an output file name.")
          |  println(">>> Is the function_type attribute filled out?")
          |
          |  return "output"
          |}
          |""".stripMargin
    }

    val setup_main_overrideIO =
      """
        |
        |def overrideIO(_params, inputs, outputs) {
        |
        |  // `inputs` in fact can be one of:
        |  // - `String`,
        |  // - `List[String]`,
        |  // - `Map[String, String | List[String]]`
        |  // Please refer to the docs for more info
        |  def overrideArgs = _params.arguments.collect{ it ->
        |    if (it.type == "file") {
        |      if (it.direction == "Input") {
        |        (inputs in List || inputs in HashMap)
        |          ? (inputs in List)
        |            ? it + [ "value" : inputs.join(it.multiple_sep)]
        |            : (inputs[it.name] != null)
        |              ? (inputs[it.name] in List)
        |                ? it + [ "value" : inputs[it.name].join(it.multiple_sep)]
        |                : it + [ "value" : inputs[it.name]]
        |              : it
        |          : it + [ "value" : inputs ]
        |      } else {
        |        (outputs in List || outputs in HashMap)
        |          ? (outputs in List)
        |            ? it + [ "value" : outputs.join(it.multiple_sep)]
        |            : (outputs[it.name] != null)
        |              ? (outputs[it.name] in List)
        |                ? it + [ "value" : outputs[it.name].join(it.multiple_sep)]
        |                : it + [ "value" : outputs[it.name]]
        |              : it
        |          : it + [ "value" : outputs ]
        |      }
        |    } else {
        |      it
        |    }
        |  }
        |
        |  def newParams = _params + [ "arguments" : overrideArgs ]
        |
        |  return newParams
        |
        |}
        |""".stripMargin

    def formatDirective(id: String, value: Option[String], delim: String): String = {
      value.map(v => s"\n  $id $delim$v$delim").getOrElse("")
    }

    /**
     * Some (implicit) conventions:
     * - `params.output/` is where the output data is published
     * - per_id is for creating directories per (sample) ID, default is true
     * - path is for modifying the layout of the output directory, default is no changes
     */
    val setup_main_process = {

      val per_idParsed = per_id.getOrElse(true)
      val pathParsed = path.map(_.split("/").mkString("/") + "/").getOrElse("")

      // If id is the empty string, the subdirectory is not created
      val publishDirString =
        if (per_idParsed) {
          s"$${params.publishDir}/$pathParsed$${id}/"
        } else {
          s"$${params.publishDir}/$pathParsed"
        }

      val publishDirStr = publish match {
        case Some(true) => s"""  publishDir "$publishDirString", mode: 'copy', overwrite: true, enabled: !params.test"""
        case _ => ""
      }

      val directives =
        labels.map(l => formatDirective("label", Some(l), "'")).mkString +
          formatDirective("label", label, "'") +
          formatDirective("cpus", directive_cpus.map(_.toString), "") +
          formatDirective("maxForks", directive_max_forks.map(_.toString), "") +
          formatDirective("time", directive_time, "'") +
          formatDirective("memory", directive_memory, "'") +
          formatDirective("cache", directive_cache, "'")

      val stageInModeStr = stageInMode match {
        case Some("copy") => "copy"
        case _ => "symlink"
      }

      s"""
        |process ${fname}_process {$directives
        |  tag "$${id}"
        |  echo { (params.debug == true) ? true : false }
        |  stageInMode "$stageInModeStr"
        |  container "$${container}"
        |$publishDirStr
        |  input:
        |    tuple val(id), path(input), val(output), val(container), val(cli), val(_params)
        |  output:
        |    tuple val("$${id}"), path(output), val(_params)
        |  stub:
        |    \"\"\"
        |    # Adding NXF's `$$moduleDir` to the path in order to resolve our own wrappers
        |    export PATH="$${moduleDir}:\\$$PATH"
        |    STUB=1 $$cli
        |    \"\"\"
        |  script:
        |    def viash_temp = System.getenv("VIASH_TEMP") ?: "/tmp/"
        |    if (params.test)
        |      \"\"\"
        |      # Some useful stuff
        |      export NUMBA_CACHE_DIR=/tmp/numba-cache
        |      # Running the pre-hook when necessary
        |      # Pass viash temp dir
        |      export VIASH_TEMP="$${viash_temp}"
        |      # Adding NXF's `$$moduleDir` to the path in order to resolve our own wrappers
        |      export PATH="./:$${moduleDir}:\\$$PATH"
        |      ./$${params.$fname.tests.testScript} | tee $$output
        |      \"\"\"
        |    else
        |      \"\"\"
        |      # Some useful stuff
        |      export NUMBA_CACHE_DIR=/tmp/numba-cache
        |      # Running the pre-hook when necessary
        |      # Pass viash temp dir
        |      export VIASH_TEMP="$${viash_temp}"
        |      # Adding NXF's `$$moduleDir` to the path in order to resolve our own wrappers
        |      export PATH="$${moduleDir}:\\$$PATH"
        |      $$cli
        |      \"\"\"
        |}
        |""".stripMargin
    }

    val emitter =
      if (separate_multiple_outputs) {
        s"""  result_
          |     | filter { it[1].keySet().size() > 1 }
          |     | view{">> Be careful, multiple outputs from this component!"}
          |
          |  emit:
          |  result_.flatMap{ it ->
          |    (it[1].keySet().size() > 1)
          |      ? it[1].collect{ k, el -> [ it[0], [ (k): el ], it[2] ] }
          |      : it[1].collect{ k, el -> [ it[0], el, it[2] ] }
          |  }""".stripMargin
      } else {
        s"""  emit:
           |  result_.flatMap{ it ->
           |    (it[1].keySet().size() > 1)
           |      ? [ it ]
           |      : it[1].collect{ k, el -> [ it[0], el, it[2] ] }
           |  }""".stripMargin
      }

    val setup_main_workflow =
      s"""
        |workflow $fname {
        |
        |  take:
        |  id_input_params_
        |
        |  main:
        |
        |  def key = "$fname"
        |
        |  def id_input_output_function_cli_params_ =
        |    id_input_params_.map{ id, input, _params ->
        |
        |      // Start from the (global) params and overwrite with the (local) _params
        |      def defaultParams = params[key] ? params[key] : [:]
        |      def overrideParams = _params[key] ? _params[key] : [:]
        |      def updtParams = defaultParams + overrideParams
        |      // Convert to List[Map] for the arguments
        |      def newParams = argumentsAsList(updtParams) + [ "id" : id ]
        |
        |      // Generate output filenames, out comes a Map
        |      def output = outFromIn(newParams)
        |
        |      // The process expects Path or List[Path], Maps need to be converted
        |      def inputsForProcess =
        |        (input in HashMap)
        |          ? input.collect{ k, v -> v }.flatten()
        |          : input
        |      def outputsForProcess = output.collect{ it.value }
        |
        |      // For our machinery, we convert Path -> String in the input
        |      def inputs =
        |        (input in List || input in HashMap)
        |          ? (input in List)
        |            ? input.collect{ it.name }
        |            : input.collectEntries{ k, v -> [ k, (v in List) ? v.collect{it.name} : v.name ] }
        |          : input.name
        |      outputs = output.collectEntries{ [(it.name): it.value] }
        |
        |      def finalParams = overrideIO(newParams, inputs, outputs)
        |
        |      checkParams(finalParams)
        |
        |      new Tuple6(
        |        id,
        |        inputsForProcess,
        |        outputsForProcess,
        |        effectiveContainer(finalParams),
        |        renderCLI([finalParams.command], finalParams.arguments),
        |        finalParams
        |      )
        |    }
        |
        |  result_ = ${fname}_process(id_input_output_function_cli_params_)
        |    | join(id_input_params_)
        |    | map{ id, output, _params, input, original_params ->
        |        def parsedOutput = _params.arguments
        |          .findAll{ it.type == "file" && it.direction == "Output" }
        |          .withIndex()
        |          .collectEntries{ it, i ->
        |            // with one entry, output is of type Path and array selections
        |            // would select just one element from the path
        |            [(it.name): (output in List) ? output[i] : output ]
        |          }
        |        new Tuple3(id, parsedOutput, original_params)
        |      }
        |
        |${emitter.replaceAll("\n", "\n|")}
        |}
        |""".stripMargin

    val resultParseBlocks: List[String] =
      if (separate_multiple_outputs && outputs >= 2) {
        allPars
          .filter(_.isInstanceOf[FileObject])
          .filter(_.direction == Output)
          .map(par =>
            s"""
              |  result \\
              |    | filter${par.plainName.capitalize} \\
              |    | view{ "Output for ${par.plainName}: " + it[1] }
              |""".stripMargin
          )
      } else {
        List("  result.view{ it[1] }")
      }

    val setup_main_entrypoint =
      s"""
        |workflow {
        |  def id = params.id
        |  def fname = "$fname"
        |
        |  def _params = params
        |
        |  // could be refactored to be FP
        |  for (entry in params[fname].arguments) {
        |    def name = entry.value.name
        |    if (params[name] != null) {
        |      params[fname].arguments[name].value = params[name]
        |    }
        |  }
        |
        |  def inputFiles = params.$fname
        |    .arguments
        |    .findAll{ key, par -> par.type == "file" && par.direction == "Input" }
        |    .collectEntries{ key, par -> [(par.name): file(params[fname].arguments[par.name].value) ] }
        |
        |  def ch_ = Channel.from("").map{ s -> new Tuple3(id, inputFiles, params)}
        |
        |  result = $fname(ch_)
        |${resultParseBlocks.mkString("\n").replaceAll("\n", "\n|")}
        |}
        |""".stripMargin

    val setup_test_entrypoint =
      s"""
        |// This workflow is not production-ready yet, we leave it in for future dev
        |// TODO
        |workflow test {
        |
        |  take:
        |  rootDir
        |
        |  main:
        |  params.test = true
        |  params.$fname.output = "$fname.log"
        |
        |  Channel.from(rootDir) \\
        |    | filter { params.$fname.tests.isDefined } \\
        |    | map{ p -> new Tuple3(
        |        "tests",
        |        params.$fname.tests.testResources.collect{ file( p + it ) },
        |        params
        |    )} \\
        |    | $fname
        |
        |  emit:
        |  $fname.out
        |}""".stripMargin

    val setup_main = PlainFile(
      dest = Some("main.nf"),
      text = Some(setup_main_header +
        setup_main_check +
        setup_main_utils +
        setup_main_outFromIn +
        setup_main_outputFilters +
        setup_main_overrideIO +
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

  trait ValueType

  case class PlainValue[A: TypeTag](v: A) extends ValueType {
    def toConfig:String = v match {
      case s: String if typeOf[String] =:= typeOf[A] =>
        quote(s)
      case b: Boolean if typeOf[Boolean] =:= typeOf[A] =>
        b.toString
      case c: Char if typeOf[Char] =:= typeOf[A]  =>
        quote(c.toString)
      case i: Int if typeOf[Int] =:= typeOf[A]  =>
        i.toString
      case l: List[_] =>
        l.map(el => quote(el.toString)).mkString("[ ", ", ", " ]")
      case _ =>
        "Parsing ERROR - Not implemented yet " + v
    }
  }

  case class ConfigTuple(tuple: (String, ValueType)) {
    def toConfig(indent: String = "  "): String = {
      val (k,v) = tuple
      v match {
        case pv: PlainValue[_] =>
          s"""$indent$k = ${pv.toConfig}"""
        case NestedValue(nv) =>
          nv.map(_.toConfig(indent + "  ")).mkString(s"$indent$k {\n", "\n", s"\n$indent}")
      }
    }
  }

  case class NestedValue(v: List[ConfigTuple]) extends ValueType

  implicit def tupleToConfigTuple[A:TypeTag](tuple: (String, A)): ConfigTuple = {
    val (k,v) = tuple
    v match {
      case NestedValue(nv) => ConfigTuple((k, NestedValue(nv)))
      case _ => ConfigTuple((k, PlainValue(v)))
    }
  }

  def listMapToConfig(m: List[ConfigTuple]): String = {
    m.map(_.toConfig()).mkString("\n")
  }

  def namespacedValueTuple(key: String, value: String)(implicit fun: Functionality): ConfigTuple =
    (s"${fun.name}__$key", value)

  implicit def dataObjectToConfigTuple[T:TypeTag](dataObject: DataObject[T])(implicit fun: Functionality): ConfigTuple = {
    val pointer = "${params." + fun.name + "__" + dataObject.plainName + "}"

    // TODO: Should this not be converted from the json?
    quoteLong(dataObject.plainName) → NestedValue(
      tupleToConfigTuple("name" → dataObject.plainName) ::
      tupleToConfigTuple("otype" → dataObject.otype) ::
      tupleToConfigTuple("required" → dataObject.required) ::
      tupleToConfigTuple("type" → dataObject.oType) ::
      tupleToConfigTuple("direction" → dataObject.direction.toString) ::
      tupleToConfigTuple("multiple" → dataObject.multiple) ::
      tupleToConfigTuple("multiple_sep" -> dataObject.multiple_sep) ::
      tupleToConfigTuple("value" → pointer) ::
      dataObject.default.map{ x =>
        List(tupleToConfigTuple("dflt" -> Bash.escape(x.toString, backtick = false, quote = true, newline = true)))
      }.getOrElse(Nil) :::
      dataObject.example.map{x =>
        List(tupleToConfigTuple("example" -> Bash.escape(x, backtick = false, quote = true, newline = true)))
      }.getOrElse(Nil) :::
      dataObject.description.map{x =>
        List(tupleToConfigTuple("description" → Bash.escape(x, backtick = false, quote = true, newline = true)))
      }.getOrElse(Nil)
    )
  }
}

// vim: tabstop=2:softtabstop=2:shiftwidth=2:expandtab
