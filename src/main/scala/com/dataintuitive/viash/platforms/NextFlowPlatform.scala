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
import com.dataintuitive.viash.platforms.requirements._
import com.dataintuitive.viash.config.Version
import com.dataintuitive.viash.helpers.Docker

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
  path: Option[String] = None,
  label: Option[String] = None,
  labels: List[String] = Nil,
  stageInMode: Option[String] = None
) extends Platform {
  val `type` = "nextflow"

  assert(version.isEmpty, "nextflow platform: attribute 'version' is deprecated")

  val requirements: List[Requirements] = Nil

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

    def inputFileExtO = allPars
      .filter(_.`type` == "file")
      .find(_.direction == Input)
      .flatMap(_.default.map(_.toString.split('.').last))

    def inputs = allPars
      .filter(_.`type` == "file")
      .count(_.direction == Input)
    def outputs = allPars
      .filter(_.`type` == "file")
      .count(_.direction == Output)

    // All values for arguments/parameters are defined in the root of
    // the params structure. the function name is prefixed as a namespace
    // identifier. A "__" is used to separate namespace and arg/option.

    val namespacedParameters: List[ConfigTuple] = {
      functionality.arguments.flatMap { dataObject => (dataObject.required, dataObject.default) match {
        case (true, Some(x)) =>
          Some(
            namespacedValueTuple(
              dataObject.plainName.replace("-", "_"),
              x.toString
            )(fun)
          )
        case (true, None) =>
          println(">>> Warning: " + dataObject.plainName + " is set to be required, but has no default value.")
          println(">>>          This will cause issues with NextFlow if this parameter is not provided explicitly.")
          None
        case (false, Some(x)) =>
          Some(
            namespacedValueTuple(
              dataObject.plainName.replace("-", "_"),
              x.toString
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
    val testPaths = tests.map(test => test.path.getOrElse("/dev/null")).toList
    val testScript: List[String] = {
        tests.flatMap{
          case test: Script => Some(test.filename)
          case _ => None
        }
    }

            // If no tests are defined, isDefined is set to FALSE
    val testConfig: List[ConfigTuple] = List("tests" -> NestedValue(
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
      "docker.runOptions" → "-i -v ${baseDir}:${baseDir}",
      "process.container" → "dataintuitive/portash",
      "params" → NestedValue(
        namespacedParameters :::
        List(
          tupleToConfigTuple("id" → ""),
          tupleToConfigTuple("input" → ""),
          tupleToConfigTuple("output" → ""),
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
        |""".stripMargin

    val setup_main_outputFilters:String =
      allPars
        .filter(_.`type` == "file")
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

    val setup_main_utils =
      s"""
        |def renderCLI(command, arguments) {
        |
        |  def argumentsList = arguments.collect{ it ->
        |    (it.otype == "")
        |      ? "\\'" + it.value + "\\'"
        |      : (it.type == "boolean_true")
        |        ? it.otype + it.name
        |        : (it.value == "no_default_value_configured")
        |          ? ""
        |          : it.otype + it.name + " \\'" + ((it.value in List && it.multiple) ? it.value.join(it.multiple_sep): it.value) + "\\'"
        |  }
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
        """
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
          |      // If a default (dflt) attribute is present, strip the extension from the filename,
          |      // otherwise just use the option name as an extension.
          |      def extOrName = (it.dflt != null) ? it.dflt.split(/\./).last() : it.name
          |      // The output filename is <sample> . <modulename> . <extension>
          |      def newName =
          |        (id != "")
          |          ? id + "." + "__f__" + "." + extOrName
          |          : "__f__" + "." + extOrName
          |      it + [ value : newName ]
          |    }
          |
          |}
          |""".stripMargin.replace("__f__", fname)
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
        case Some(true) => s"""  publishDir "$publishDirString", mode: 'copy', overwrite: true, enabled: !params.test"""
        case _ => ""
      }

      val labelsString = labels.map(l => s"  label '$l'").mkString("\n")

      val labelString = label match {
        case Some(str) => s"  label '$str'"
        case _ => ""
      }

      val stageInModeStr = stageInMode match {
        case Some("copy") => "copy"
        case _ => "symlink"
      }

      s"""
        |process ${fname}_process {
        |$labelsString
        |$labelString
        |  tag "$${id}"
        |  echo { (params.debug == true) ? true : false }
        |  cache 'deep'
        |  stageInMode "$stageInModeStr"
        |  container "$${container}"
        |$publishDirStr
        |  input:
        |    tuple val(id), path(input), val(output), val(container), val(cli), val(_params)
        |  output:
        |    tuple val("$${id}"), path(output), val(_params)
        |  script:
        |    if (params.test)
        |      \"\"\"
        |      # Some useful stuff
        |      export NUMBA_CACHE_DIR=/tmp/numba-cache
        |      # Running the pre-hook when necessary
        |      # Adding NXF's `$$moduleDir` to the path in order to resolve our own wrappers
        |      export PATH="./:$${moduleDir}:\\$$PATH"
        |      ./$${params.${fname}.tests.testScript} | tee $$output
        |      \"\"\"
        |    else
        |      \"\"\"
        |      # Some useful stuff
        |      export NUMBA_CACHE_DIR=/tmp/numba-cache
        |      # Running the pre-hook when necessary
        |      # Adding NXF's `$$moduleDir` to the path in order to resolve our own wrappers
        |      export PATH="$${moduleDir}:\\$$PATH"
        |      $$cli
        |      \"\"\"
        |}
        |""".stripMargin
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
        |      new Tuple6(
        |        id,
        |        inputsForProcess,
        |        outputsForProcess,
        |        effectiveContainer(finalParams),
        |        renderCLI([finalParams.command], finalParams.arguments),
        |        finalParams
        |        )
        |    }
        |
        |  result_ = ${fname}_process(id_input_output_function_cli_params_) \\
        |    | join(id_input_params_) \\
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
        |  result_ \\
        |    | filter { it[1].keySet().size() > 1 } \\
        |    | view{
        |        ">> Be careful, multiple outputs from this component!"
        |    }
        |
        |  emit:
        |  result_.flatMap{ it ->
        |    (it[1].keySet().size() > 1)
        |      ? it[1].collect{ k, el -> [ it[0], [ (k): el ], it[2] ] }
        |      : it[1].collect{ k, el -> [ it[0], el, it[2] ] }
        |  }
        |}
        |""".stripMargin

    val resultParseBlocks:List[String] =
      if (outputs >= 2) {
        allPars
          .filter(_.`type` == "file")
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
        |
        |  def id = params.id
        |  def _params = argumentsAsList(params.${fname}) + [ "id" : id ]
        |  def p = _params
        |    .arguments
        |    .findAll{ it.type == "file" && it.direction == "Input" }
        |    .collectEntries{ [(it.name): file(params[it.name]) ] }
        |
        |  def ch_ = Channel.from("").map{ s -> new Tuple3(id, p, params)}
        |
        |  result = $fname(ch_)
        |""".stripMargin +
      resultParseBlocks.mkString("\n") +
      s"""
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
        |  params.${fname}.output = "${fname}.log"
        |
        |  Channel.from(rootDir) \\
        |    | filter { params.${fname}.tests.isDefined } \\
        |    | map{ p -> new Tuple3(
        |        "tests",
        |        params.${fname}.tests.testResources.collect{ file( p + it ) },
        |        params
        |    )} \\
        |    | ${fname}
        |
        |  emit:
        |  ${fname}.out
        |}""".stripMargin

    val setup_main = PlainFile(
      dest = Some("main.nf"),
      text = Some(setup_main_header +
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

    val valueOrPointer: String = {
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
      tupleToConfigTuple("value" → valueOrPointer) ::
      dataObject.default
        .map(x => List(tupleToConfigTuple("dflt" -> x.toString))).getOrElse(Nil) :::
      dataObject.description
        .map(x => List(tupleToConfigTuple("description" → x.toString))).getOrElse(Nil)
      )
  }
}

// vim: tabstop=2:softtabstop=2:shiftwidth=2:expandtab
