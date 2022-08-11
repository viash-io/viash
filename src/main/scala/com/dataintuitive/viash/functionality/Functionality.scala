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

import arguments._
import resources._
import io.viash.config.Version
import io.circe.generic.extras._
import io.viash.helpers._

@description("""The functionality-part of the config file describes the behaviour of the script in terms of arguments and resources.
               |By specifying a few restrictions (e.g. mandatory arguments) and adding some descriptions, Viash will automatically generate a stylish command-line interface for you.
               |""".stripMargin)
case class Functionality(
  @description("Name of the component and the filename of the executable when built with `viash build`.")
  @example("name: exe", "yaml")
  name: String,

  @description("Namespace this component is a part of. This is required when grouping components together in a pipeline and building multiple components at once using viash `ns build`.")
  @example("namespace: fancy_components", "yaml")
  namespace: Option[String] = None,

  @description("Version of the component. This field will be used to version the executable and the Docker container.")
  version: Option[Version] = None,

  @description("""A list of authors. An author must at least have a name, but can also have a list of roles, an e-mail address, and a map of custom properties.
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
  @since("Viash 0.3.1")
  authors: List[Author] = Nil,

  @since("Viash 0.5.11")
  inputs: List[Argument[_]] = Nil,

  @since("Viash 0.5.11")
  outputs: List[Argument[_]] = Nil,
  
  @description("""A list of arguments for this component. For each argument, a type and a name must be specified. Depending on the type of argument, different properties can be set. Common properties for all argument types are the following.
                 |
                 | - `type: string/file/integer/double/boolean/boolean_true/boolean_false`, the type of argument determining to what object type the value will be cast in the downstream scripts.
                 | - `direction: input/output`, the directionality of the argument. Only needs to be specified for output files. Default: “input”.
                 | - `name: --foo`, the name of the argument. Can also be `-foo` or `foo`. The number of dashes determines how values can be passed:
                 |   - with `--foo`: long option, e.g. `exe --foo=bar` or exe `--foo bar`
                 |   - with `-foo`: short option, e.g. `exe -foo bar`
                 |   - with `foo`: argument, e.g. `exe bar`
                 | - `alternatives: [-f]`, list of alternative names. Typically only used to provide a short alternative option.
                 | - `description: Description of foo`, a description of the argument. Multiline descriptions are supported.
                 | - `default: bar`, the default value when no argument value is provided. Not allowed when `required: true`.
                 | - `required: true/false`, whether the argument is required. If true and the functionality is executed, an error will be produced if no value is provided. Default = false.
                 | - `multiple: true/false`, whether to treat the argument value as an array or not. Arrays can be passed using the delimiter `--foo=1:2:3` or by providing the same argument multiple times `--foo 1 --foo 2`. Default = false.
                 | - `multiple_sep: ":"`, the delimiter for providing multiple values. Default = “:”.
                 | - `must_exist: true/false`, denotes whether the file or folder should exist at the start of the execution. Only when 'type' is 'file'.
                 |
                 |On types: 
                 |
                 | * `type: string`, The value passed through an argument of this type is converted to an ‘str’ object in Python, and to a ‘character’ object in R.
                 | * `type: integer`, The resulting value is an ‘int’ in Python and an ‘integer’ in R.
                 | * `type: double`, The resulting value is a ‘float’ in Python and an ‘double’ in R.
                 | * `type: boolean`, The resulting value is a ‘bool’ in Python and a ‘logical’ in R.
                 | * `type: boolean_true/boolean_false`, Arguments of this type can only be used by providing a flag `--foo` or not. The resulting value is a ‘bool’ in Python and a ‘logical’ in R. These properties cannot be altered: required is false, default is undefined, multiple is false.
                 | * `type: file`, The resulting value is still an ‘str’ in Python and a ‘character’ in R. In order to correctly pass files in some platforms (e.g. Docker and Nextflow), Viash needs to know which arguments are input/output files.
                 |
                 |""".stripMargin)
  @example("""- name: --foo
             |  type: file
             |  alternatives: [-f]
             |  description: Description of foo
             |  default: "/foo/bar"
             |  must_exist: true
             |  required: false
             |  multiple: true
             |  multiple_sep: ","
             |""".stripMargin, "yaml")
  arguments: List[Argument[_]] = Nil,

  @description("""A grouping of the arguments, used to display the help message.
                 |
                 | - `name: foo`, the name of the argument group. 
                 | - `description: Description of foo`, a description of the argument group. Multiline descriptions are supported.
                 | - `arguments: [arg1, arg2, ...]`, list of the arguments names.
                 |
                 |""".stripMargin)
  @example("""- name: "Input"
             |  arguments: [ id, input1, input2 ]
             |- name: "Output"
             |  arguments: [ output, optional_output ]
             |- name: "Foo"
             |  description: Arguments related to the foo functionality of this component.
             |  arguments: [ foo, bar, zing, bork ]
             |""".stripMargin, "yaml")
  @since("Viash 0.5.14")
  argument_groups: List[ArgumentGroup] = Nil,

  @description("""The first resource should be a script (bash_script, r_script, python_script, javascript_script, scala_script) which is what will be executed when the functionality is run. Additional resources will be copied to the same directory.
                 |
                 |Common properties:
                 |
                 | * type: file/r_script/python_script/bash_script/javascript_script/scala_script, the type of resource. The first resource cannot be of type file. When the type is not specified, the default type is simply file. For more information regarding how to write a script in Bash, R or Python with Viash, check out the guides for the respective languages on the left.
                 | * name: filename, the resulting name of the resource.
                 | * path: path/to/file, the path of the input file. Can be a relative or an absolute path, or a URI.
                 | * text: ...multiline text..., the raw content of the input file. Exactly one of path or text must be defined, the other undefined.
                 | * is_executable: true/false, whether the resulting file is made executable.
                 |""".stripMargin)
  @example("""resources:
             |  - type: r_script
             |    path: script.R
             |  - type: file
             |    path: resource1.txt
             |""".stripMargin, "yaml")
  resources: List[Resource] = Nil,

  @description("A description of the component. This will be displayed with --help.")
  @example("""description: |
             +  This component performs function Y and Z.
             +  It is possible to make this a multiline string.
             +""".stripMargin('+'), "yaml")
  description: Option[String] = None,

  @description("A description of the component. This will be displayed with --help under the 'Usage:' section.")
  usage: Option[String] = None,

  @description("""One or more Bash/R/Python scripts to be used to test the component behaviour when `viash test` is invoked. Additional files of type `file` will be made available only during testing. Each test script should expect no command-line inputs, be platform-independent, and return an exit code >0 when unexpected behaviour occurs during testing.""")
  @example("""tests:
             |  - type: bash_script
             |    path: tests/test1.sh
             |  - type: r_script
             |    path: tests/test2.R
             |  - path: resource1.txt
             |""".stripMargin, "yaml")
  test_resources: List[Resource] = Nil,

  @description("A map for storing custom annotation.")
  @since("Viash 0.4.0")
  info: Map[String, String] = Map.empty[String, String],

  // dummy arguments are used for handling extra directory mounts in docker
  dummy_arguments: List[Argument[_]] = Nil,

  // setting this to true will change the working directory
  // to the resources directory when running the script
  // this is used when running `viash test`.
  set_wd_to_resources_dir: Boolean = false,

  @description("Setting this to false with disable this component when using namespaces.")
  @since("Viash 0.5.13")
  enabled: Boolean = true,

  @description("""Computational requirements related to running the component. 
    |`n_proc` specifies the maximum number of processes a component is allowed to spawn in parallel, whereas
    |`memory` specifies the maximum amount of memory a component is allowed to allicate. Memory units must be
    |in B, KB, MB, GB, TB or PB.""".stripMargin)
  @example("""requirements:
             |  n_proc: 5
             |  memory: 10GB
             |""".stripMargin, "yaml")
  @since("Viash 0.5.16")
  requirements: ComputationalRequirements = ComputationalRequirements()
) {
  // START OF REMOVED PARAMETERS THAT ARE STILL DOCUMENTED
  @description("Adds the resources directory to the PATH variable when set to true. This is set to false by default.")
  @since("Viash 0.5.5")
  @removed("Extending the PATH turned out to be not desirable.", "Viash 0.5.11")
  private val add_resources_to_path: Boolean = false

  @description("One or more Bash/R/Python scripts to be used to test the component behaviour when `viash test` is invoked. Additional files of type `file` will be made available only during testing. Each test script should expect no command-line inputs, be platform-independent, and return an exit code >0 when unexpected behaviour occurs during testing.")
  @deprecated("Use `test_resources` instead. No functional difference.", "Viash 0.5.13")
  private val tests: List[Resource] = Nil
  // END OF REMOVED PARAMETERS THAT ARE STILL DOCUMENTED

  // note that in the Functionality companion object, defaults gets added to inputs and outputs *before* actually 
  // parsing the configuration file with Circe. This is done in the .prepare step.
  inputs.foreach { input =>
    require(input.direction == Input, s"input ${input.name} can only have input as direction")
  }
  outputs.foreach { output =>
    require(output.direction == Output, s"input ${output.name} can only have output as direction")
  }

  // Combine inputs, outputs and arguments into one combined list
  def allArguments = inputs ::: outputs ::: arguments

  // check argument groups
  {
    val allArgumentNames = allArguments.map(_.plainName)
    for (group <- argument_groups; argument <- group.arguments) {
      require(allArgumentNames.contains(argument), s"group '${group.name}' has unknown argument '$argument'")
    }
    argument_groups.flatMap(_.arguments).groupBy(identity).foreach { case (arg, args) => 
      require(args.length == 1, s"argument '${arg}' can be in at most one argument group")
    }
  }

  private def addToArgGroup(argumentGroups: List[ArgumentGroup], name: String, arguments: List[Argument[_]]): List[ArgumentGroup] = {
    val argNamesInGroups = argumentGroups.flatMap(_.arguments).toSet

    // Check if 'arguments' is in 'argumentGroups'. 
    val argumentsNotInGroup = arguments.map(_.plainName).filter(argName => !argNamesInGroups.contains(argName))

    // Check whether an argument group of 'name' exists.
    val existing = argumentGroups.find(gr => name == gr.name)

    // if there are no arguments missing from the argument group, just return the existing group (if any)
    if (argumentsNotInGroup.isEmpty) {
      existing.toList

    // if there are missing arguments and there is an existing group, add the missing arguments to it
    } else if (existing.isDefined) {
      List(existing.get.copy(
        arguments = existing.get.arguments.toList ::: argumentsNotInGroup
      ))
    
    // else create a new group
    } else {
      List(ArgumentGroup(
        name = name,
        arguments = argumentsNotInGroup
      ))
    }
  }

  def allArgumentGroups: List[ArgumentGroup] = {
    val inputGroup = addToArgGroup(argument_groups, "Inputs", inputs)
    val outputGroup = addToArgGroup(argument_groups, "Outputs", outputs)
    val defaultGroup = addToArgGroup(argument_groups, "Arguments", arguments)
    val groupsFiltered = argument_groups.filter(gr => !List("Inputs", "Outputs", "Arguments").contains(gr.name))

    inputGroup ::: outputGroup ::: groupsFiltered ::: defaultGroup
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

  def allArgumentsAndDummies: List[Argument[_]] = allArguments ::: dummy_arguments
}

object Functionality {
  val reservedParameters = List("-h", "--help", "--version", "---v", "---verbose", "---verbosity")
}
