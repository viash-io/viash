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

package io.viash.config

import io.viash.config_mods.ConfigModParser
import arguments._
import resources._
import dependencies._
import io.viash.platforms.Platform
import io.viash.helpers.{Git, GitInfo, IO, Logging}
import io.viash.helpers.circe._
import io.viash.helpers.{status => BuildStatus};
import io.viash.helpers.Yaml

import java.net.URI

import java.io.File
import io.viash.config_mods.ConfigMods
import java.nio.file.Paths
import io.circe.Json

import io.viash.schemas._
import java.io.ByteArrayOutputStream
import java.nio.file.FileSystemNotFoundException
import io.viash.runners.{Runner, ExecutableRunner, NextflowRunner}
import io.viash.engines.{Engine, NativeEngine, DockerEngine}
import io.viash.helpers.ReplayableMultiOutputStream
import io.viash.packageConfig.PackageConfig
import io.viash.lenses.ConfigLenses._
import Status._
import io.viash.wrapper.BashWrapper
import scala.collection.immutable.ListMap

@description(
  """A Viash configuration is a YAML file which contains metadata to describe the behaviour and build target(s) of a component.  
    |We commonly name this file `config.vsh.yaml` in our examples, but you can name it however you choose.  
    |""".stripMargin)
@example(
  """name: hello_world
    |arguments:
    |  - type: string
    |    name: --input
    |    default: "world"
    |resources:
    |  - type: bash_script
    |    path: script.sh
    |    text: echo Hello $par_input
    |runners:
    |  - type: executable
    |engines:
    |  - type: docker
    |    image: "bash:4.0"
    |""".stripMargin, "yaml")
case class Config(
  // @description(
  //   """The @[functionality](functionality) describes the behaviour of the script in terms of arguments and resources.
  //     |By specifying a few restrictions (e.g. mandatory arguments) and adding some descriptions, Viash will automatically generate a stylish command-line interface for you.
  //     |""".stripMargin)
  // functionality: Functionality,
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
      |in B, KB, MB, GB, TB or PB.""".stripMargin)
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
    """(Pre-)defines repositories that can be used as repository in dependencies.
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
  // The variables below are for internal use and shouldn't be publicly documented

  // setting this to true will change the working directory
  // to the resources directory when running the script
  // this is used when running `viash test`.
  @internalFunctionality
  set_wd_to_resources_dir: Boolean = false,

  @description(
    """A list of runners to execute target artifacts.
      |
      | - @[ExecutableRunner](executable_runner)
      | - @[NextflowRunner](nextflow_runner)
      |""".stripMargin)
  @since("Viash 0.8.0")
  @default("Empty")
  runners: List[Runner] = Nil,
  @description(
    """A list of engine environments to execute target artifacts in.
      |
      | - @[NativeEngine](native_engine)
      | - @[DockerEngine](docker_engine)
      |""".stripMargin)
  @since("Viash 0.8.0")
  @default("Empty")
  engines: List[Engine] = Nil,

  @undocumented
  build_info: Option[BuildInfo] = None,

  @description("The package config content used during build.")
  @since("Viash 0.9.0")
  @undocumented
  package_config: Option[PackageConfig] = None,
) {

  @description(
    """Config inheritance by including YAML partials. This is useful for defining common APIs in
      |separate files. `__merge__` can be used in any level of the YAML. For example,
      |not just in the config but also in the functionality or any of the engines.
      |""".stripMargin)
  @example("__merge__: ../api/common_interface.yaml", "yaml")
  @since("Viash 0.6.3")
  private val `__merge__`: Option[File] = None

  @description(
  """A list of platforms to generate target artifacts for.
    |
    | - @[Native](platform_native)
    | - @[Docker](platform_docker)
    | - @[Nextflow](platform_nextflow)
    |""".stripMargin)
  @default("Empty")
  @deprecated("Use 'engines' and 'runners' instead.", "0.9.0", "0.10.0")
  private val platforms: List[Platform] = Nil
  
  /**
    * Find the runner
    * 
    * Order of execution:
    *   - if an runner id is passed, look up the runner in the runners list
    *   - else if runners is a non-empty list, use the first runner
    *   - else use the executable runner
    *
    * @param runnerStr An runner ID referring to one of the config's runners
    * @return An runner
    */
  def findRunner(query: Option[String]): Runner = {
    findRunners(query).head
  }

  /**
    * Find the runners
    * 
    * Order of execution:
    *   - if an runner id is passed, look up the runner in the runners list
    *   - else if runners is a non-empty list, use the first runner
    *   - else use the executable runner
    *
    * @param query An runner ID referring to one of the config's runners
    * @return An runner
    */
  def findRunners(query: Option[String]): List[Runner] = {
    // TODO: match on query, there's no need to do a .* if query is None
    val regex = query.getOrElse(".*").r

    val foundMatches = runners.filter{ e =>
      regex.findFirstIn(e.id).isDefined
    }
    
    foundMatches match {
      case li if li.nonEmpty =>
        li
      case Nil if query.isDefined =>
        throw new RuntimeException(s"no runner id matching regex '$regex' could not be found in the config.")
      case _ =>
        // TODO: switch to the getRunners.head ?
        List(ExecutableRunner())
    }
  }

  /**
    * Find the engines
    * 
    * Order of execution:
    *   - if an engine id is passed, look up the engine in the engines list
    *   - else if engines is a non-empty list, use the first engine
    *   - else use the executable engine
    *
    * @param query An engine ID referring to one of the config's engines
    * @return An engine
    */
  def findEngines(query: Option[String]): List[Engine] = {
    // TODO: match on query, there's no need to do a .* if query is None
    val regex = query.getOrElse(".*").r

    val foundMatches = engines.filter{ e =>
      regex.findFirstIn(e.id).isDefined
    }
    
    foundMatches match {
      case li if li.nonEmpty =>
        li
      case Nil if query.isDefined =>
        throw new RuntimeException(s"no engine id matching regex '$regex' could not be found in the config.")
      case _ =>
        // TODO: switch to the getEngines.head ?
        List(NativeEngine())
    }
  }


  // Combine all arguments into one combined list
  def allArguments = argument_groups.flatMap(arg => arg.arguments)
    
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

  // check component name
  require(name.matches("^[A-Za-z][A-Za-z0-9_]*$"), message = "component name must begin with a letter and consist only of alphanumeric characters or underscores.")

  // check arguments
  {
    val allNames = allArguments.map(a => a.name) ::: allArguments.flatMap(a => a.alternatives)
    val allNamesCounted = allNames.groupBy(identity).map(a => (a._1, a._2.length))

    allArguments.foreach { arg =>
      require(arg.name.matches("^(-?|--|\\$)[A-Za-z][A-Za-z0-9_]*$"), message = s"argument $arg.name: name must begin with a letter and consist only of alphanumeric characters or underscores.")
      (arg.name :: arg.alternatives).foreach { argName =>
        require(!Config.reservedParameters.contains(argName), message = s"argument $argName: name is reserved by viash")
        require(!argName.matches("^\\$VIASH_"), message = s"argument $argName: environment variables beginning with 'VIASH_' are reserved for viash.")
        require(allNamesCounted(argName) == 1, message = s"argument $argName: name or alternative name is not unique.")
      }
    }
  }

  def getArgumentLikes(includeMeta: Boolean = false, includeDependencies: Boolean = false, filterInputs: Boolean = false, filterOutputs: Boolean = false): List[Argument[_]] = {
    // start with arguments
    val args0 = allArguments

    // add meta if need be
    val args1 = args0 ++ { if (includeMeta) BashWrapper.metaArgs else Nil }

    // add dependencies if need be
    val args2 = args1 ++ { if (includeDependencies) dependencies.map( d => StringArgument(d.scriptName, required = false, dest = "dep") ) else Nil }
    
    // filter input files if need be
    val args3 = if (filterInputs) args2.filter{d => d.direction == Input || d.isInstanceOf[FileArgument]} else args2
    
    // filter output files if need be
    val args4 = if (filterOutputs) args3.filter{d => d.direction == Output || d.isInstanceOf[FileArgument]} else args3

    args4
  }
  def getArgumentLikesGroupedByDest(includeMeta: Boolean = false, includeDependencies: Boolean = false, filterInputs: Boolean = false, filterOutputs: Boolean = false): ListMap[String, List[Argument[_]]] = {
    val x = getArgumentLikes(includeMeta, includeDependencies, filterInputs, filterOutputs).groupBy(_.dest)
    val y = Seq("par", "meta", "dep").map(k => (k, x.getOrElse(k, Nil)))
    ListMap(y: _*)
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

object Config extends Logging {
  def readYAML(config: String): (String, Option[Script]) = {
    val configUri = IO.uri(config)
    readYAML(configUri)
  }

  def readYAML(configUri: URI): (String, Option[Script]) = {
    // get config text
    val configStr = IO.read(configUri)
    val configUriStr = configUri.toString

    // get extension
    val extension = configUriStr.substring(configUriStr.lastIndexOf(".") + 1).toLowerCase()

    // get basename
    val basenameRegex = ".*/".r
    val basename = basenameRegex.replaceFirstIn(configUriStr, "")

    // detect whether a script (with joined header) was passed or a joined yaml
    // using the extension
    if ((extension == "yml" || extension == "yaml") && configStr.contains("name:")) {
      (configStr, None)
    } else if (Script.extensions.contains(extension)) {
      // detect scripting language from extension
      val scriptObj = Script.fromExt(extension)

      // check whether viash header contains a name
      val commentStr = scriptObj.commentStr + "'"
      val headerComm = commentStr + " "
      assert(
        configStr.contains(s"$commentStr name:"),
        message = s"""viash script should contain a name header: "$commentStr name: <...>""""
      )

      // split header and body
      val (header, body) = configStr.split("\n").partition(_.startsWith(headerComm))
      val yaml = header.map(s => s.drop(3)).mkString("\n")
      val code = body.mkString("\n")

      val script = Script(dest = Some(basename), text = Some(code), `type` = scriptObj.`type`)

      (yaml, Some(script))
    } else {
      throw new RuntimeException("config file (" + configUri + ") must be a yaml file containing a viash config.")
    }
  }

  // reads and modifies the config based on the current setup
  def read(
    configPath: String,
    addOptMainScript: Boolean = true,
    viashPackage: Option[PackageConfig] = None,
  ): Config = {
    val uri = IO.uri(configPath)
    readFromUri(
      uri = uri,
      addOptMainScript = addOptMainScript,
      viashPackage = viashPackage
    )
  }

  def readFromUri(
    uri: URI,
    addOptMainScript: Boolean = true,
    viashPackage: Option[PackageConfig] = None,
  ): Config = {
    val packageDir = viashPackage.flatMap(_.rootDir).map(_.toUri())
    val configMods: List[String] = viashPackage.map(_.config_mods.toList).getOrElse(Nil)

    // read cli config mods
    val confMods = ConfigMods.parseConfigMods(configMods)

    /* STRING */
    // read yaml as string
    val (yamlText, optScript) = readYAML(uri)

    // replace valid yaml definitions for +.inf with "+.inf" so that circe doesn't trip over its toes
    val replacedYamlText = Yaml.replaceInfinities(yamlText)
    
    /* JSON 0: parsed from string */
    // parse yaml into Json
    val json0 = Convert.textToJson(replacedYamlText, uri.toString())

    /* JSON 1: after inheritance */
    // apply inheritance if need be
    val json1 = json0.inherit(uri, packageDir)

    /* JSON 2: after preparse config mods  */
    // apply preparse config mods if need be
    val json2 = confMods(json1, preparse = true)

    /* CONFIG Base: converted from json */
    // convert Json into Config
    val confBase = Convert.jsonToClass[Config](json2, uri.toString())

    /* CONFIG 0: apply values from package config */
    // apply values from package config if need be
    val conf0 = {
      val vpVersion = viashPackage.flatMap(_.version)
      val vpRepositories = viashPackage.map(_.repositories).getOrElse(Nil)
      val vpLicense = viashPackage.flatMap(_.license)
      val vpOrganization = viashPackage.flatMap(_.organization)
      val vpRepository = viashPackage.flatMap(_.links.repository)
      val vpDockerRegistry = viashPackage.flatMap(_.links.docker_registry)

      val lenses =
        versionLens.modify(_ orElse vpVersion) andThen
        licenseLens.modify(_ orElse vpLicense) andThen
        organizationLens.modify(_ orElse vpOrganization) andThen
        repositoriesLens.modify(vpRepositories ::: _) andThen
        linksRepositoryLens.modify(_ orElse vpRepository) andThen
        linksDockerRegistryLens.modify(_ orElse vpDockerRegistry)
        
      lenses(confBase)
    }

    /* CONFIG 1: apply post-parse config mods */
    // apply config mods only if need be
    val conf1 = 
      if (confMods.postparseCommands.nonEmpty) {
        // turn config back into json
        val js = encodeConfig(conf0)
        // apply config mods
        val modifiedJs = confMods(js, preparse = false)
        // turn json back into a config
        Convert.jsonToClass[Config](modifiedJs, uri.toString())
      } else {
        conf0
      }

    /* CONFIG 2: store parent path in resource to be able to access them in the future */
    // copy resources with updated paths into config and return
    val parentURI = uri.resolve("")
    val resources = conf1.resources.map(_.copyWithAbsolutePath(parentURI, packageDir))
    val tests = conf1.test_resources.map(_.copyWithAbsolutePath(parentURI, packageDir))

    val conf2a = resourcesLens.set(resources)(conf1)
    val conf2 = testResourcesLens.set(tests)(conf2a)

    /* CONFIG 3: add info */
    // gather git info
    // todo: resolve git in package?
    val conf3 = uri.getScheme() match {
      case "file" =>
        val path = Paths.get(uri).toFile().getParentFile
        val GitInfo(_, rgr, gc, gt) = Git.getInfo(path)

        // create info object
        val info = 
          BuildInfo(
            viash_version = Some(io.viash.Main.version),
            config = uri.toString.replaceAll("^file:/+", "/"),
            git_commit = gc,
            git_remote = rgr,
            git_tag = gt
          )
        // add info and additional resources
        conf2.copy(
          build_info = Some(info),
        )
      case _ => conf2
    }

    // print warnings if need be
    if (conf3.status == Status.Deprecated)
      warn(s"Warning: The status of the component '${conf3.name}' is set to deprecated.")
    
    if (conf3.resources.isEmpty && optScript.isEmpty)
      warn(s"Warning: no resources specified!")

    /* CONFIG 4: add Viash Package Config */
    val conf4 = conf3.copy(
      package_config = viashPackage
    )

    if (!addOptMainScript) {
      return conf4
    }
    
    /* CONFIG 5: add main script if config is stored inside script */
    // add info and additional resources
    val conf5 = resourcesLens.modify(optScript.toList ::: _)(conf4)

    conf5
  }

  def readConfigs(
    source: String,
    query: Option[String] = None,
    queryNamespace: Option[String] = None,
    queryName: Option[String] = None,
    addOptMainScript: Boolean = true,
    viashPackage: Option[PackageConfig] = None,
  ): List[AppliedConfig] = {

    val sourceDir = Paths.get(source)

    // find [^\.]*.vsh.* files and parse as config
    val scriptFiles = IO.find(sourceDir, (path, attrs) => {
      path.toString.contains(".vsh.") &&
        !path.toFile.getName.startsWith(".") &&
        attrs.isRegularFile
    })

    scriptFiles.map { file =>
      try {
        val rmos = new ReplayableMultiOutputStream()

        val appliedConfig: AppliedConfig = 
          // Don't output any warnings now. Only output warnings if the config passes the regex checks.
          // When replaying, output directly to the console. Logger functions were already called, so we don't want to call them again.
          Console.withErr(rmos.getOutputStream(Console.err.print)) {
            Console.withOut(rmos.getOutputStream(Console.out.print)) {
              read(
                file.toString,
                addOptMainScript = addOptMainScript,
                viashPackage = viashPackage
              )
            }
          }

        val funName = appliedConfig.config.name
        val funNs = appliedConfig.config.namespace
        val isEnabled = appliedConfig.config.isEnabled

        // does name & namespace match regex?
        val queryTest = (query, funNs) match {
          case (Some(regex), Some(ns)) => regex.r.findFirstIn(ns + "/" + funName).isDefined
          case (Some(regex), None) => regex.r.findFirstIn(funName).isDefined
          case (None, _) => true
        }
        val nameTest = queryName match {
          case Some(regex) => regex.r.findFirstIn(funName).isDefined
          case None => true
        }
        val namespaceTest = (queryNamespace, funNs) match {
          case (Some(regex), Some(ns)) => regex.r.findFirstIn(ns).isDefined
          case (Some(_), None) => false
          case (None, _) => true
        }

        if (!isEnabled) {
          appliedConfig.setStatus(BuildStatus.Disabled)
        } else if (queryTest && nameTest && namespaceTest) {
          // if config passes regex checks, show warnings if there are any
          rmos.replay()
          appliedConfig
        } else {
          appliedConfig.setStatus(BuildStatus.DisabledByQuery)
        }
      } catch {
        case _: Exception =>
          error(s"Reading file '$file' failed")
          AppliedConfig(Config("failed"), None, Nil, Some(BuildStatus.ParseError))
      }
    }
  }

val reservedParameters = List("-h", "--help", "--version", "---v", "---verbose", "---verbosity")
}
