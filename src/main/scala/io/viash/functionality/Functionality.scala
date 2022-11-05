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

package io.viash.functionality


import io.circe.Json
import io.circe.generic.extras._
import arguments._
import resources._
import Status._
import io.viash.schemas._
import io.viash.wrapper.BashWrapper

@description(
  """The functionality-part of the config file describes the behaviour of the script in terms of arguments and resources.
    |By specifying a few restrictions (e.g. mandatory arguments) and adding some descriptions, Viash will automatically generate a stylish command-line interface for you.
    |""".stripMargin)
case class Functionality(
  @description("Name of the component and the filename of the executable when built with `viash build`.")
  @example("name: this_is_my_component", "yaml")
  name: String,

  @description("Namespace this component is a part of. See the @[namespace](Namespaces guide) for more information on namespaces.")
  @example("namespace: fancy_components", "yaml")
  namespace: Option[String] = None,

  @description("Version of the component. This field will be used to version the executable and the Docker container.")
  @example("version: 0.8", "yaml")
  version: Option[String] = None,

  @description(
    """A list of authors. An author must at least have a name, but can also have a list of roles, an e-mail address, and a map of custom properties.
      +
      +Suggested values for roles are:
      + 
      +| Role | Abbrev. | Description |
      +|------|---------|-------------|
      +| maintainer | mnt | for the maintainer of the code. Ideally, exactly one maintainer is specified. |
      +| author | aut | for persons who have made substantial contributions to the software. |
      +| contributor | ctb| for persons who have made smaller contributions (such as code patches).
      +| datacontributor | dtc | for persons or organisations that contributed data sets for the software
      +| copyrightholder | cph | for all copyright holders. This is a legal concept so should use the legal name of an institution or corporate body.
      +| funder | fnd | for persons or organizations that furnished financial support for the development of the software
      +
      +The [full list of roles](https://www.loc.gov/marc/relators/relaterm.html) is extremely comprehensive.
      +""".stripMargin('+'))
  @example(
    """authors:
      |  - name: Bob Cando
      |    roles: [maintainer, author]
      |    email: bob@can.do
      |    props: {github: bobcando, orcid: 0000-0001-0002-0003}
      |  - name: Tim Farbe
      |    roles: [author]
      |    email: tim@far.be""".stripMargin,
      "yaml")
  @since("Viash 0.3.1")
  authors: List[Author] = Nil,

  @description("A list of input arguments in addition to the `arguments` list. Any arguments specified here will have their `type` set to `file` and the `direction` set to `input` by default.")
  @example(
    """inputs:
      |  - name: input_file
      |  - name: another_input""".stripMargin,
      "yaml")
  @exampleWithDescription(
    """component_with_inputs
      |  
      |  Inputs:
      |      input_file
      |          type: file
      |  
      |      another_input
      |          type: file""".stripMargin,
      "bash",
      "This results in the following output when calling the component with the `--help` argument:")
  @since("Viash 0.5.11")
  @deprecated("Use `arguments` instead.", "Viash 0.6.0")
  inputs: List[Argument[_]] = Nil,

  @description("A list of output arguments in addition to the `arguments` list. Any arguments specified here will have their `type` set to `file` and thr `direction` set to `output` by default.")
  @example(
    """outputs:
      |  - name: output_file
      |  - name: another_output""".stripMargin,
      "yaml")
  @exampleWithDescription(
    """component_with_outputs
      |  
      |  Outputs:
      |      output_file
      |          type: file, output
      |  
      |      another_output
      |          type: file, output""".stripMargin,
      "bash",
      "This results in the following output when calling the component with the `--help` argument:")
  @since("Viash 0.5.11")
  @deprecated("Use `arguments` instead.", "Viash 0.6.0")
  outputs: List[Argument[_]] = Nil,
  
  @description(
    """A list of arguments for this component. For each argument, a type and a name must be specified. Depending on the type of argument, different properties can be set. See these reference pages per type for more information:  
      |
      | - @[arg_string](string)
      | - @[arg_file](file)
      | - @[arg_integer](integer)
      | - @[arg_double](double)
      | - @[arg_boolean](boolean)
      | - @[arg_boolean_true](boolean_true)
      | - @[arg_boolean_false](boolean_false)
      |""".stripMargin)
  @example(
    """arguments:
      |   - name: --foo
      |    type: file
      |    alternatives: [-f]
      |    description: Description of foo
      |    default: "/foo/bar"
      |    must_exist: true
      |    direction: output
      |    required: false
      |    multiple: true
      |    multiple_sep: ","
      |   - name: --bar
      |    type: string
      |""".stripMargin,
      "yaml")
  arguments: List[Argument[_]] = Nil,

  @description(
    """A grouping of the arguments, used to display the help message.
      |
      | - `name: foo`, the name of the argument group. 
      | - `description: Description of foo`, a description of the argument group. Multiline descriptions are supported.
      | - `arguments: [arg1, arg2, ...]`, list of the arguments names.
      |
      |""".stripMargin)
  @example(
    """argument_groups:
      |  - name: "Input"
      |    arguments:
      |      - name: "--id"
      |        type: string
      |        required: true
      |      - name: "--input"
      |        type: file
      |        required: true
      |  - name: "Output"
      |    arguments:
      |      - name: "--output"
      |        type: file
      |        direction: output
      |        required: true
      |      - name: "--output_optional"
      |        type: file
      |        direction: output
      |""".stripMargin,
      "yaml")
  @exampleWithDescription(
    """component_name
      |
      |  Input:
      |      --id
      |          type: string
      |
      |      --input
      |          type: file
      |
      |  Output:
      |      --output
      |          type: file
      |
      |      --optional_output
      |          type: file
      |""".stripMargin,
      "bash",
      "This results in the following output when calling the component with the `--help` argument:")
  @since("Viash 0.5.14")
  argument_groups: List[ArgumentGroup] = Nil,

  @description(
    """@[resources](Resources) are files that support the component. The first resource should be @[scripting_languages](a script) that will be executed when the functionality is run. Additional resources will be copied to the same directory.
      |
      |Common properties:
      |
      | * type: `file` / `r_script` / `python_script` / `bash_script` / `javascript_script` / `scala_script` / `csharp_script`, the type of resource. The first resource cannot be of type `file`. When the type is not specified, the default type is simply `file`.
      | * name: filename, the resulting name of the resource.
      | * path: `path/to/file`, the path of the input file. Can be a relative or an absolute path, or a URI.
      | * text: ...multiline text..., the raw content of the input file. Exactly one of path or text must be defined, the other undefined.
      | * is_executable: `true` / `false`, whether the resulting file is made executable.
      |""".stripMargin)
  @example(
    """resources:
      |  - type: r_script
      |    path: script.R
      |  - type: file
      |    path: resource1.txt
      |""".stripMargin,
      "yaml")
  resources: List[Resource] = Nil,

  @description("A description of the component. This will be displayed with `--help`.")
  @example(
    """description: |
      +  This component performs function Y and Z.
      +  It is possible to make this a multiline string.
      +""".stripMargin('+'),
      "yaml")
  description: Option[String] = None,

  @description("A description on how to use the component. This will be displayed with `--help` under the 'Usage:' section.")
  @example("usage: Place the executable in a directory containing TSV files and run it", "yaml")
  usage: Option[String] = None,

  @description("""One or more @[scripting_language](scripts) to be used to test the component behaviour when `viash test` is invoked. Additional files of type `file` will be made available only during testing. Each test script should expect no command-line inputs, be platform-independent, and return an exit code >0 when unexpected behaviour occurs during testing. See @[unit_testing](Unit Testing) for more info.""")
  @example(
    """test_resources:
      |  - type: bash_script
      |    path: tests/test1.sh
      |  - type: r_script
      |    path: tests/test2.R
      |  - path: resource1.txt
      |""".stripMargin,
      "yaml")
  test_resources: List[Resource] = Nil,

  @description("Structured information. Can be any shape: a string, vector, map or even nested map.")
  @example(
    """info:
      |  twitter: wizzkid
      |  classes: [ one, two, three ]""".stripMargin, "yaml")
  @since("Viash 0.4.0")
  info: Json = Json.Null,

  @description("Allows setting a component to active, deprecated or disabled.")
  @since("Viash 0.6.0")
  status: Status = Status.Enabled,
  
  @description(
    """Computational requirements related to running the component. 
      |`cpus` specifies the maximum number of (logical) cpus a component is allowed to use., whereas
      |`memory` specifies the maximum amount of memory a component is allowed to allicate. Memory units must be
      |in B, KB, MB, GB, TB or PB.""".stripMargin)
  @example(
    """requirements:
      |  cpus: 5
      |  memory: 10GB
      |""".stripMargin,
      "yaml")
  @since("Viash 0.6.0")
  requirements: ComputationalRequirements = ComputationalRequirements(),

  // The variables below are for internal use and shouldn't be publicly documented

  // setting this to true will change the working directory
  // to the resources directory when running the script
  // this is used when running `viash test`.
  @internalFunctionality
  set_wd_to_resources_dir: Boolean = false
) {
  // START OF REMOVED PARAMETERS THAT ARE STILL DOCUMENTED
  @description("Adds the resources directory to the PATH variable when set to true. This is set to false by default.")
  @since("Viash 0.5.5")
  @removed("Extending the PATH turned out to be not desirable.", "Viash 0.5.11")
  private val add_resources_to_path: Boolean = false

  @description("One or more Bash/R/Python scripts to be used to test the component behaviour when `viash test` is invoked. Additional files of type `file` will be made available only during testing. Each test script should expect no command-line inputs, be platform-independent, and return an exit code >0 when unexpected behaviour occurs during testing.")
  @deprecated("Use `test_resources` instead. No functional difference.", "Viash 0.5.13")
  private val tests: List[Resource] = Nil

  @description("Setting this to false with disable this component when using namespaces.")
  @since("Viash 0.5.13")
  @deprecated("Use `status` instead.", "Viash 0.6.0")
  private val enabled: Boolean = true
  // END OF REMOVED PARAMETERS THAT ARE STILL DOCUMENTED
  if (inputs.nonEmpty) {
    Console.err.println("Warning: .functionality.inputs is deprecated. Please use .functionality.arguments instead.")
  }
  if (outputs.nonEmpty) {
    Console.err.println("Warning: .functionality.outputs is deprecated. Please use .functionality.arguments instead.")
  }

  // note that in the Functionality companion object, defaults gets added to inputs and outputs *before* actually 
  // parsing the configuration file with Circe. This is done in the .prepare step.
  inputs.foreach { input =>
    require(input.direction == Input, s"input ${input.name} can only have input as direction")
  }
  outputs.foreach { output =>
    require(output.direction == Output, s"input ${output.name} can only have output as direction")
  }

  // Combine inputs, outputs and arguments into one combined list
  def allArguments = inputs ::: outputs ::: arguments ::: argument_groups.flatMap(_.argumentArguments)

  // check argument groups
  {
    val allArgumentNames = allArguments.map(_.plainName)
    for (group <- argument_groups; argument <- group.stringArguments) {
      require(allArgumentNames.contains(argument), s"group '${group.name}' has unknown argument '$argument'")
    }
    argument_groups.flatMap(_.stringArguments).groupBy(identity).foreach { case (arg, args) => 
      require(args.length == 1, s"argument '${arg}' can be in at most one argument group")
    }
  }

  private def addToArgGroup(argumentGroups: List[ArgumentGroup], name: String, arguments: List[Argument[_]]): List[ArgumentGroup] = {
    val argNamesInGroups = argumentGroups.flatMap(_.stringArguments).toSet

    // Check if 'arguments' is in 'argumentGroups'. 
    val argumentsNotInGroup = arguments.filter(arg => !argNamesInGroups.contains(arg.plainName))

    // Check whether an argument group of 'name' exists.
    val existing = argumentGroups.find(gr => name == gr.name)

    // if there are no arguments missing from the argument group, just return the existing group (if any)
    if (argumentsNotInGroup.isEmpty) {
      existing.toList

    // if there are missing arguments and there is an existing group, add the missing arguments to it
    } else if (existing.isDefined) {
      List(existing.get.copy(
        arguments = existing.get.arguments.toList ::: argumentsNotInGroup.map(arg => Right(arg))
      ))
    
    // else create a new group
    } else {
      List(ArgumentGroup(
        name = name,
        arguments = argumentsNotInGroup.map(arg => Right(arg))
      ))
    }
  }

  def allArgumentGroups: List[ArgumentGroup] = {
    val inputGroup = addToArgGroup(argument_groups, "Inputs", inputs)
    val outputGroup = addToArgGroup(argument_groups, "Outputs", outputs)
    val defaultGroup = addToArgGroup(argument_groups, "Arguments", arguments)
    val groupsFiltered = argument_groups.filter(gr => !List("Inputs", "Outputs", "Arguments").contains(gr.name))

    inputGroup ::: outputGroup ::: defaultGroup ::: groupsFiltered
  }
    
  // check whether there are not multiple positional arguments with multiplicity >1
  // and if there is one, whether its position is last
  {
    val positionals = allArguments.filter(a => a.flags == "")
    val multiix = positionals.indexWhere(_.multiple)

    require(
      multiix == -1 || multiix == positionals.length - 1,
      message = s"positional argument ${positionals(multiix).name} should be last since it has multiplicity >1"
    )
  }

  // check functionality name
  require(name.matches("^[A-Za-z][A-Za-z0-9_]*$"), message = "functionality name must begin with a letter and consist only of alphanumeric characters or underscores.")

  // check arguments
  {
    val allNames = allArguments.map(a => a.name) ::: allArguments.flatMap(a => a.alternatives)
    val allNamesCounted = allNames.groupBy(identity).map(a => (a._1, a._2.length))

    allArguments.foreach { arg =>
      require(arg.name.matches("^(-?|--|\\$)[A-Za-z][A-Za-z0-9_]*$"), message = s"argument $arg.name: name must begin with a letter and consist only of alphanumeric characters or underscores.")
      (arg.name :: arg.alternatives).foreach { argName =>
        require(!Functionality.reservedParameters.contains(argName), message = s"argument $argName: name is reserved by viash")
        require(!argName.matches("^\\$VIASH_"), message = s"argument $argName: environment variables beginning with 'VIASH_' are reserved for viash.")
        require(allNamesCounted(argName) == 1, message = s"argument $argName: name or alternative name is not unique.")
      }
    }
  }

  def getArgumentLikes(includeMeta: Boolean = false, filterInputs: Boolean = false, filterOutputs: Boolean = false): List[Argument[_]] = {
    // start with arguments
    val args0 = allArguments

    // add meta if need be
    val args1 = args0 ++ { if (includeMeta) BashWrapper.metaArgs else Nil }
    
    // filter input files if need be
    val args2 = if (filterInputs) args1.filter{d => d.direction == Input || d.isInstanceOf[FileArgument]} else args1
    
    // filter output files if need be
    val args3 = if (filterOutputs) args2.filter{d => d.direction == Output || d.isInstanceOf[FileArgument]} else args2

    args3
  }
  def getArgumentLikesGroupedByDest(includeMeta: Boolean = false, filterInputs: Boolean = false, filterOutputs: Boolean = false): Map[String, List[Argument[_]]] = {
    val x = getArgumentLikes(includeMeta, filterInputs, filterOutputs).groupBy(_.dest)
    val y = Map("par" -> Nil, "meta" -> Nil)
    (x.toSeq ++ y.toSeq).groupBy(_._1).map { 
      case (k, li) => (k, li.flatMap(_._2).toList) 
    }
  }

  def mainScript: Option[Script] =
    resources.headOption.flatMap {
      case s: Script => Some(s)
      case _ => None
    }
  def mainCode: Option[String] = mainScript.flatMap(_.read)
  // provide function to use resources.tail but that allows resources to be an empty list
  def additionalResources = resources match {
    case _ :: tail => tail
    case _ => List.empty[Resource]
  }

  def isEnabled: Boolean = status != Status.Disabled
}

object Functionality {
  val reservedParameters = List("-h", "--help", "--version", "---v", "---verbose", "---verbosity")
}
