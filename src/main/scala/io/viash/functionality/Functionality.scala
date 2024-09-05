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
import io.viash.config.arguments._
import io.viash.config.resources._
import io.viash.config.Status._
import io.viash.config.dependencies._
import io.viash.config.{Author, ArgumentGroup, ComputationalRequirements, Status, References, Links}
import io.viash.schemas._
import io.viash.wrapper.BashWrapper
import scala.collection.immutable.ListMap

@description(
  """The functionality-part of the config file describes the behaviour of the script in terms of arguments and resources.
    |By specifying a few restrictions (e.g. mandatory arguments) and adding some descriptions, Viash will automatically generate a stylish command-line interface for you.
    |""".stripMargin)
@deprecated("Functionality level is deprecated, all functionality fields are now located on the top level of the config file.", "0.9.0", "0.10.0")
case class Functionality(
  @description("Name of the component and the filename of the executable when built with `viash build`.")
  @example("name: this_is_my_component", "yaml")
  name: String,

  @description("Namespace this component is a part of. See the @[Namespaces guide](namespace) for more information on namespaces.")
  @example("namespace: fancy_components", "yaml")
  namespace: Option[String] = None,

  @description("Version of the component. This field will be used to version the executable and the Docker container.")
  @example("version: 0.8", "yaml")
  version: Option[String] = None,

  @description(
    """A list of @[authors](author). An author must at least have a name, but can also have a list of roles, an e-mail address, and a map of custom properties.
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
      |  - name: Jane Doe
      |    role: [author, maintainer]
      |    email: jane@doe.com
      |    info:
      |      github: janedoe
      |      twitter: janedoe
      |      orcid: XXAABBCCXX
      |      groups: [ one, two, three ]
      |  - name: Tim Farbe
      |    roles: [author]
      |    email: tim@far.be
      |""".stripMargin, "yaml")
  @since("Viash 0.3.1")
  @default("Empty")
  authors: List[Author] = Nil,

  @description(
    """A grouping of the @[arguments](argument), used to display the help message.
      |
      | - `name: foo`, the name of the argument group. 
      | - `description: Description of foo`, a description of the argument group. Multiline descriptions are supported.
      | - `arguments: [arg1, arg2, ...]`, list of the arguments.
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
  @default("Empty")
  argument_groups: List[ArgumentGroup] = Nil,

  @description(
    """@[Resources](resources) are files that support the component. The first resource should be @[a script](scripting_languages) that will be executed when the functionality is run. Additional resources will be copied to the same directory.
      |
      |Common properties:
      |
      | * type: `file` / `r_script` / `python_script` / `bash_script` / `javascript_script` / `scala_script` / `csharp_script`, specifies the type of the resource. The first resource cannot be of type `file`. When the type is not specified, the default type is simply `file`.
      | * dest: filename, the resulting name of the resource.  From within a script, the file can be accessed at `meta["resources_dir"] + "/" + dest`. If unspecified, `dest` will be set to the basename of the `path` parameter.
      | * path: `path/to/file`, the path of the input file. Can be a relative or an absolute path, or a URI. Mutually exclusive with `text`.
      | * text: ...multiline text..., the content of the resulting file specified as a string. Mutually exclusive with `path`.
      | * is_executable: `true` / `false`, whether the resulting resource file should be made executable.
      |""".stripMargin)
  @example(
    """resources:
      |  - type: r_script
      |    path: script.R
      |  - type: file
      |    path: resource1.txt
      |""".stripMargin,
      "yaml")
  @default("Empty")
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

  @description("""One or more @[scripts](scripting_languages) to be used to test the component behaviour when `viash test` is invoked. Additional files of type `file` will be made available only during testing. Each test script should expect no command-line inputs, be platform-independent, and return an exit code >0 when unexpected behaviour occurs during testing. See @[Unit Testing](unit_testing) for more info.""")
  @example(
    """test_resources:
      |  - type: bash_script
      |    path: tests/test1.sh
      |  - type: r_script
      |    path: tests/test2.R
      |  - path: resource1.txt
      |""".stripMargin,
      "yaml")
  @default("Empty")
  test_resources: List[Resource] = Nil,

  @description("Structured information. Can be any shape: a string, vector, map or even nested map.")
  @example(
    """info:
      |  twitter: wizzkid
      |  classes: [ one, two, three ]""".stripMargin, "yaml")
  @since("Viash 0.4.0")
  @default("Empty")
  info: Json = Json.Null,

  @description("Allows setting a component to active, deprecated or disabled.")
  @since("Viash 0.6.0")
  @default("Enabled")
  status: Status = Status.Enabled,
  
  @description(
    """@[Computational requirements](computational_requirements) related to running the component. 
      |`cpus` specifies the maximum number of (logical) cpus a component is allowed to use., whereas
      |`memory` specifies the maximum amount of memory a component is allowed to allicate. Memory units must be
      |in B, KB, MB, GB, TB or PB for SI units (1000-base), or KiB, MiB, GiB, TiB or PiB for binary IEC units (1024-base).""".stripMargin)
  @example(
    """requirements:
      |  cpus: 5
      |  memory: 10GB
      |""".stripMargin,
      "yaml")
  @since("Viash 0.6.0")
  @default("Empty")
  requirements: ComputationalRequirements = ComputationalRequirements(),

  @description("Allows listing Viash components required by this Viash component")
  @exampleWithDescription(
    """dependencies:
      |  - name: qc/multiqc
      |    repository: 
      |      type: github
      |      uri: openpipelines-bio/modules
      |      tag: 0.3.0
      |""".stripMargin,
    "yaml",
    "Full specification of a repository")
  @exampleWithDescription(
    """dependencies:
      |  - name: qc/multiqc
      |    repository: "github://openpipelines-bio/modules:0.3.0"
      |""".stripMargin,
    "yaml",
    "Full specification of a repository using sugar syntax")
  @exampleWithDescription(
    """dependencies:
      |  - name: qc/multiqc
      |    repository: "openpipelines-bio"
      |""".stripMargin,
    "yaml",
    "Reference to a repository fully specified under 'repositories'")
  @default("Empty")
  dependencies: List[Dependency] = Nil,

  @description(
    """(Pre-)defines @[repositories](repository) that can be used as repository in dependencies.
      |Allows reusing repository definitions in case it is used in multiple dependencies.""".stripMargin)
  @example(
    """repositories:
      |  - name: openpipelines-bio
      |    type: github
      |    uri: openpipelines-bio/modules
      |    tag: 0.3.0
      |""".stripMargin,
      "yaml")
  @default("Empty")
  repositories: List[RepositoryWithName] = Nil,

  @description("The keywords of the components.")
  @example("keywords: [ bioinformatics, genomics ]", "yaml")
  @default("Empty")
  @since("Viash 0.9.0")
  keywords: List[String] = Nil,

  @description("The license of the package.")
  @example("license: MIT", "yaml")
  @default("Empty")
  @since("Viash 0.9.0")
  license: Option[String] = None,

  @description("The organization of the package.")
  @example("organization: viash-io", "yaml")
  @default("Empty")
  @since("Viash 0.9.0")
  organization: Option[String] = None,

  @description("References to external resources related to the component.")
  @example(
    """references:
      |  doi: 10.1000/xx.123456.789
      |  bibtex: |
      |    @article{foo,
      |      title={Foo},
      |      author={Bar},
      |      journal={Baz},
      |      year={2024}
      |    }
      |""".stripMargin, "yaml")
  @default("Empty")
  @since("Viash 0.9.0")
  references: References = References(),

  @description("External links of the component.")
  @example(
    """links:
      |  repository: "https://github.com/viash-io/viash"
      |  docker_registry: "https://ghcr.io"
      |  homepage: "https://viash.io"
      |  documentation: "https://viash.io/reference/"
      |  issue_tracker: "https://github.com/viash-io/viash/issues"
      |""".stripMargin, "yaml")
  @default("Empty")
  @since("Viash 0.9.0")
  links: Links = Links(),

  // Allow arguments to be listed here, as Functionality it is non-functional and the arguments are merged in argument_groups in pre-parsing.
  // Previously listed as a private val in the Functionality class.
  // Listing it here greatly simplifies the validation of Functionality and has no downsides.
  @description(
    """A list of @[arguments](argument) for this component. For each argument, a type and a name must be specified. Depending on the type of argument, different properties can be set. See these reference pages per type for more information:  
      |
      | - @[string](arg_string)
      | - @[file](arg_file)
      | - @[integer](arg_integer)
      | - @[double](arg_double)
      | - @[boolean](arg_boolean)
      | - @[boolean_true](arg_boolean_true)
      | - @[boolean_false](arg_boolean_false)
      |""".stripMargin)
  @example(
    """arguments:
      |  - name: --foo
      |    type: file
      |    alternatives: [-f]
      |    description: Description of foo
      |    default: "/foo/bar"
      |    must_exist: true
      |    direction: output
      |    required: false
      |    multiple: true
      |    multiple_sep: ";"
      |  - name: --bar
      |    type: string
      |""".stripMargin,
      "yaml")
  @default("Empty")
  arguments: List[Argument[_]] = Nil,
)

