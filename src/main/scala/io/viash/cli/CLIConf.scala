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

package io.viash.cli

import org.rogach.scallop._
import io.viash.Main
import io.viash.exceptions.ExitException
import io.viash.helpers.Logging

trait ViashCommand {
  _: DocumentedSubcommand =>
  val runner = registerOpt[String](
    name = "runner",
    default = None,
    descr =
      "Specifies which runner amongst those specified in the config to use. " +
      "If this is not provided, the first runner will be used. " +
      "If no runners are defined in the config, the executable runner will be used.",
    required = false
  )
  val engine = registerOpt[String](
    name = "engine",
    default = None,
    descr =
      "A regex to determine which engines amongst those specified in the config to use. " +
      "If this is not provided, all engines will be used. " +
      "If no engines are defined in the config, the native engine will be used.",
    required = false
  )
  val config = registerTrailArg[String](
    name = "config",
    descr = "A viash config file (example: `config.vsh.yaml`). This argument can also be a script with the config as a header.",
    default = Some("config.vsh.yaml"),
    required = true
  )
  val config_mods = registerOpt[List[String]](
    name = "config_mod",
    short = Some('c'),
    default = Some(Nil),

    descr = "Modify a viash config at runtime using @[dynamic config modding](config_mod)."
  )
}
trait ViashRunner {
  _: DocumentedSubcommand =>
  val cpus = registerOpt[Int](
    name = "cpus",
    default = None,
    descr = "The maximum number of (logical) cpus a component is allowed to use.",
    required = false
  )
  val memory = registerOpt[String](
    name = "memory",
    descr = "The maximum amount of memory a component is allowed to allocate. Unit must be one of B, KB, MB, GB, TB or PB.",
    default = None,
    required = false
  )
}
trait ViashNs {
  _: DocumentedSubcommand =>
  val query = registerOpt[String](
    name = "query",
    short = Some('q'),
    descr = "Filter which components get selected by component and @[namespace](namespace) name. Can be a regex. Example: \"^mynamespace/component1$\".",
    default = None
  )
  val query_namespace = registerOpt[String](
    name = "query_namespace",
    short = Some('n'),
    descr = "Filter which namespaces get selected by @[namespace](namespace) name. Can be a regex. Example: \"^mynamespace$\".",
    default = None
  )
  val query_name = registerOpt[String](
    name = "query_name",
    descr = "Filter which components get selected by component name. Can be a regex. Example: \"^component1\".",
    default = None
  )
  val src = registerOpt[String](
    name = "src",
    short = Some('s'),
    descr = "A source directory containing viash config files, possibly structured in a hierarchical folder structure. Default: src/.",
    default = None
  )
  val runner = registerOpt[String](
    name = "runner",
    default = None,
    descr =
      "Acts as a regular expression to filter the runner ids specified in the found config files. " +
        "If this is not provided, all runners will be used. " +
        "If no runners are defined in a config, the executable runner will be used.",
    required = false
  )
  val engine = registerOpt[String](
    name = "engine",
    default = None,
    descr =
      "Acts as a regular expression to filter the engine ids specified in the found config files. " +
        "If this is not provided, all engines will be used. " +
        "If no engines are defined in a config, the native engine will be used.",
    required = false
  )
  val parallel = registerOpt[Boolean](
    name = "parallel",
    short = Some('l'),
    default = Some(false),
    descr = "Whether or not to run the process in parallel."
  )
  val config_mods = registerOpt[List[String]](
    name = "config_mod",
    short = Some('c'),
    default = Some(Nil),
    descr = "Modify a viash config at runtime using @[dynamic config modding](config_mod)."
  )
}

trait ViashNsBuild {
  _: DocumentedSubcommand =>
  val target = registerOpt[String](
    name = "target",
    short = Some('t'),
    descr = "A target directory to build the executables into. Default: target/.",
    default = None
  )
}
trait WithTemporary {
  _: DocumentedSubcommand =>
  val keep = registerOpt[String](
    name = "keep",
    short = Some('k'),
    default = None,
    descr = "Whether or not to keep temporary files. By default, files will be deleted if all goes well but remain when an error occurs." +
      " By specifying --keep true, the temporary files will always be retained, whereas --keep false will always delete them." +
      " The temporary directory can be overwritten by setting defining a VIASH_TEMP directory."
  )
}

trait ViashLogger {
  _: DocumentedSubcommand =>
  val colorize = registerChoice(
    name = "colorize",
    short = None,
    descr = "Specify whether the console output should be colorized. If not specified, we attempt to detect this automatically.",
    choices = List("true", "false", "auto"),
    hidden = true
  )
  val loglevel = registerChoice(
    name = "loglevel",
    short = None,
    descr = "Specify the log level in use",
    choices = List("error", "warn", "info", "debug", "trace"),
    hidden = true
  )
}

class CLIConf(arguments: Seq[String]) extends ScallopConf(arguments) with Logging {
  def getRegisteredCommands(includeHidden: Boolean = false) = subconfigs.flatMap{ sc =>
    sc match {
      case ds: DocumentedSubcommand if !ds.hidden || includeHidden => Some(ds.toRegisteredCommand)
      case _ => None
    }
  }

  exitHandler = (i: Int) => throw new ExitException(i)
  stderrPrintln = (s: String) => logger.info(s)
  stdoutPrintln = (s: String) => logger.infoOut(s)
 
  version(s"${Main.name} ${Main.version} (c) 2020 Data Intuitive")

  appendDefaultToDescription = true

  banner(
    s"""
       |viash is a spec and a tool for defining execution contexts and converting execution instructions to concrete instantiations.
       |
       |This program comes with ABSOLUTELY NO WARRANTY. This is free software, and you are welcome to redistribute it under certain conditions. For more information, see our license at the link below.
       |  https://github.com/viash-io/viash/blob/master/LICENSE.md
       |
       |Usage:
       |  viash run config.vsh.yaml -- [arguments for script]
       |  viash build config.vsh.yaml
       |  viash test config.vsh.yaml
       |  viash ns build
       |  viash ns test
       |  viash ns list
       |  viash config view config.vsh.yaml
       |  viash config inject config.vsh.yaml
       |
       |Check the help of a subcommand for more information, or the API available at:
       |  https://viash.io
       |
       |Arguments:""".stripMargin)

  val run = new DocumentedSubcommand("run") with ViashCommand with WithTemporary with ViashRunner with ViashLogger {
    banner(
      "viash run",
      "Executes a viash component from the provided viash config file. viash generates a temporary executable and immediately executes it with the given parameters.",
      "viash run config.vsh.yaml [-p docker] [-k true/false]  -- [arguments for script]")

    footer(
      s"""  -- param1 param2 ...    Extra parameters to be passed to the component itself.
         |                          -- is used to separate viash arguments from the arguments
         |                          of the component.
         |
         |The temporary directory can be altered by setting the VIASH_TEMP directory. Example:
         |  export VIASH_TEMP=/home/myuser/.viash_temp
         |  viash run config.vsh.yaml""".stripMargin)
  }

  val build = new DocumentedSubcommand("build") with ViashCommand with ViashLogger {
    banner(
      "viash build",
      "Build an executable from the provided viash config file.",
      "viash build config.vsh.yaml -o output [-p docker] [-m] [-s]")

    val output = registerOpt[String](
      name = "output",
      short = Some('o'),
      descr = "Path to directory in which the executable and any resources is built to. Default: \"output/\".",
      default = Some("output/"),
      required = true
    )
    val setup = registerOpt[String](
      name = "setup",
      short = Some('s'),
      default = None,
      descr = "Which @[setup strategy](docker_setup_strategy) for creating the container to use [Docker Engine only]."
    )
    val push = registerOpt[Boolean](
      name = "push",
      default = Some(false),
      descr = "Whether or not to push the container to a Docker registry [Docker Engine only]."
    )
  }

  val test = new DocumentedSubcommand("test") with ViashCommand with WithTemporary with ViashRunner with ViashLogger {
    banner(
      "viash test",
      "Test the component using the tests defined in the viash config file.",
      "viash test config.vsh.yaml [-p docker] [-k true/false] [--setup cachedbuild]")
    
    val setup = registerOpt[String](
      name = "setup",
      short = Some('s'),
      default = None,
      descr = "Which @[setup strategy](docker_setup_strategy) for creating the container to use [Docker Engine only]."
    )
    
    footer(
      s"""
         |The temporary directory can be altered by setting the VIASH_TEMP directory. Example:
         |  export VIASH_TEMP=/home/myuser/.viash_temp
         |  viash run meta.vsh.yaml""".stripMargin)
  }

  val config = new DocumentedSubcommand("config") {
    val view = new DocumentedSubcommand("view") with ViashCommand with ViashLogger {
      banner(
        "viash config view",
        "View the config file after parsing.",
        "viash config view config.vsh.yaml")

      val format = registerChoice(
        name = "format",
        short = Some('f'),
        default = Some("yaml"),
        choices = List("yaml", "json"),
        descr = "Which output format to use."
      )
      val parse_argument_groups = registerOpt[Boolean](
        name = "parse_argument_groups",
        default = Some(false),
        descr = "Whether or not to postprocess each component's @[argument groups](argument_groups)."
      )
    }
    val inject = new DocumentedSubcommand("inject") with ViashCommand with ViashLogger {
      banner(
        "viash config inject",
        "Inject a Viash header into the main script of a Viash component.",
        "viash config inject config.vsh.yaml")
    }

    addSubcommand(view)
    addSubcommand(inject)
    requireSubcommand()

    shortSubcommandsHelp(true)
  }

  val namespace = new DocumentedSubcommand("ns") {

    val build = new DocumentedSubcommand("build") with ViashNs with ViashNsBuild with ViashLogger {
      banner(
        "viash ns build",
        "Build a namespace from many viash config files.",
        "viash ns build [-n nmspc] [-s src] [-t target] [-p docker] [--setup] [--push] [--parallel] [--flatten]")

      val setup = registerOpt[String](
        name = "setup",
        default = None,
        descr = "Which @[setup strategy](docker_setup_strategy) for creating the container to use [Docker Engine only]."
      )
      val push = registerOpt[Boolean](
        name = "push",
        default = Some(false),
        descr = "Whether or not to push the container to a Docker registry [Docker Engine only]."
      )
      val flatten = registerOpt[Boolean](
        name = "flatten",
        short = Some('f'),
        default = Some(false),
        descr = "Flatten the target builds, handy for building one runner/engine to a bin directory."
      )
    }

    val test = new DocumentedSubcommand("test") with ViashNs with WithTemporary with ViashRunner with ViashLogger {
      banner(
        "viash ns test",
        "Test a namespace containing many viash config files.",
        "viash ns test [-n nmspc] [-s src] [-p docker] [--parallel] [--tsv file.tsv] [--setup cachedbuild] [--append]")

      val setup = registerOpt[String](
        name = "setup",
        default = None,
        descr = "Which @[setup strategy](docker_setup_strategy) for creating the container to use [Docker Engine only]."
      )

      val tsv = registerOpt[String](
        name = "tsv",
        short = Some('t'),
        descr = "Path to write a summary of the test results to."
      )
      val append = registerOpt[Boolean](
        name = "append",
        short = Some('a'),
        default = Some(false),
        descr = "Append to tsv instead of overwrite"
      )
    }

    val list = new DocumentedSubcommand("list") with ViashNs with ViashLogger {
      banner(
        "viash ns list",
        "List a namespace containing many viash config files.",
        "viash ns list [-n nmspc] [-s src] [-p docker]")

      val format = registerChoice(
        name = "format",
        short = Some('f'),
        default = Some("yaml"),
        choices = List("yaml", "json"),
        descr = "Which output format to use."
      )
      val parse_argument_groups = registerOpt[Boolean](
        name = "parse_argument_groups",
        default = Some(false),
        descr = "Whether or not to postprocess each component's @[argument groups](argument_groups)."
      )
    }

    val exec = new DocumentedSubcommand("exec") with ViashNs with ViashLogger {
      banner(
        "viash ns exec",
        """Execute a command for all found Viash components.
          |The syntax of this command is inspired by `find . -exec echo {} \;`.
          |
          |The following fields are automatically replaced:  
          |
          | * `{}` | `{path}`: path to the config file
          | * `{abs-path}`: absolute path to the config file
          | * `{dir}`: path to the parent directory of the config file
          | * `{abs-dir}`: absolute path to the directory of the config file
          | * `{main-script}`: path to the main script (if any)
          | * `{abs-main-script}`: absolute path to the main script (if any)
          | * `{functionality-name}`: name of the component
          | * `{namespace}`: namespace of the component
          | * `{runner}`: selected runner id (only when --apply_runner is used)
          | * `{engine}`: selected engine id (only when --apply_engine is used)
          | * `{output}`: path to the destination directory when building the component
          | * `{abs-output}`: absolute path to the destination directory when building the component
          |
          |A command suffixed by `\;` (or nothing) will execute one command for each
          |of the Viash components, whereas a command suffixed by `+` will execute one
          |command for all Viash components.""".stripMargin,
        """viash ns exec 'echo {path} \\;'
          |viash ns exec 'chmod +x {main-script} +'""".stripMargin
      )
      val applyRunner = registerOpt[Boolean] (
        name = "apply_runner",
        short = Some('r'),
        default = Some(false),
        descr = 
          """Fills in the {runner} and {output} field by applying each runner to the 
            |config separately. Note that this results in the provided command being applied
            |once for every runner that matches the --runner regex.""".stripMargin
      )
      val applyEngine = registerOpt[Boolean] (
        name = "apply_engine",
        short = Some('e'),
        default = Some(false),
        descr = 
          """Fills in the {engine} and {output} field by applying each engine to the 
            |config separately. Note that this results in the provided command being applied
            |once for every engine that matches the --engine regex.""".stripMargin
      )

      val dryrun = registerOpt[Boolean] (
        name = "dry_run",
        short = Some('d'),
        default = Some(false),
        descr = "Perform a dry run."
      )

      val cmd = registerTrailArg[String](
        name = "cmd",
        descr = "The command to execute for each viash config file in the namespace.",
        required = true
      )
    }

    addSubcommand(build)
    addSubcommand(test)
    addSubcommand(list)
    addSubcommand(exec)
    requireSubcommand()

    shortSubcommandsHelp(true)
  }

  val `export` = new DocumentedSubcommand("export") {
    hidden = true

    val resource = new DocumentedSubcommand("resource") with ViashLogger {
      banner(
        "viash export resource",
        """Export an internal resource file""".stripMargin,
        """viash export resource runners/nextflow/WorkflowHelper.nf [--output foo.nf]""".stripMargin
      )

      val path = registerTrailArg[String](
        name = "path",
        descr = "Path to an internal resource file",
        required = true,
        default = None
      )
      val output = registerOpt[String](
        name = "output",
        default = None,
        descr = "Destination path"
      )
    }

    val cli_schema = new DocumentedSubcommand("cli_schema") with ViashLogger {
      banner(
        "viash export cli_schema",
        """Export the schema of the Viash CLI as a JSON""".stripMargin,
        """viash export cli_schema [--output file.json] [--format json]""".stripMargin
      )
      val output = registerOpt[String](
        name = "output",
        default = None,
        descr = "Destination path"
      )
      val format = registerChoice(
        name = "format",
        short = Some('f'),
        default = Some("yaml"),
        choices = List("yaml", "json"),
        descr = "Which output format to use."
      )
    }

    val cli_autocomplete = new DocumentedSubcommand("cli_autocomplete") with ViashLogger {
      banner(
        "viash export bash_autocomplete",
        """Export the autocomplete script as to be used in Bash or Zsh""".stripMargin,
        """viash export bash_autocomplete [--output viash_autocomplete_bash] [--zsh]""".stripMargin
      )
      val output = registerOpt[String](
        name = "output",
        default = None,
        descr = "Destination path"
      )
      val format = registerChoice(
        name = "format",
        short = Some('f'),
        default = Some("bash"),
        choices = List("bash", "zsh"),
        descr = "Which autocomplete format to use."
      )
    }

    val config_schema = new DocumentedSubcommand("config_schema") with ViashLogger {
      banner(
        "viash export config_schema",
        """Export the schema of a Viash config as a JSON""".stripMargin,
        """viash export config_schema [--output file.json] [--format json]""".stripMargin
      )
      val output = registerOpt[String](
        name = "output",
        default = None,
        descr = "Destination path"
      )
      val format = registerChoice(
        name = "format",
        short = Some('f'),
        default = Some("yaml"),
        choices = List("yaml", "json"),
        descr = "Which output format to use."
      )
    }

    val json_schema = new DocumentedSubcommand("json_schema") with ViashLogger {
      banner(
        "viash export json_schema",
        """Export the json schema to validate a Viash config""".stripMargin,
        """viash export json_schema [--output file.json] [--format json]""".stripMargin
      )
      val output = registerOpt[String](
        name = "output",
        default = None,
        descr = "Destination path"
      )
      val format = registerChoice(
        name = "format",
        short = Some('f'),
        default = Some("yaml"),
        choices = List("yaml", "json"),
        descr = "Which output format to use."
      )
      val strict = registerOpt[Boolean](
        name = "strict",
        default = Some(false),
        descr = "Whether or not to use export the strict schema variant."
      )
      val minimal = registerOpt[Boolean](
        name = "minimal",
        default = Some(false),
        descr = "Whether or not to output extra schema annotations."
      )
    }

    addSubcommand(resource)
    addSubcommand(cli_schema)
    addSubcommand(cli_autocomplete)
    addSubcommand(config_schema)
    addSubcommand(json_schema)
    requireSubcommand()

    shortSubcommandsHelp(true)
  }

  addSubcommand(run)
  addSubcommand(build)
  addSubcommand(test)
  addSubcommand(namespace)
  addSubcommand(config)
  addSubcommand(export)

  shortSubcommandsHelp(true)

  // remove 'export' command from help
  // see https://github.com/scallop/scallop/issues/228
  helpFormatter = new ScallopHelpFormatter {
    override def getShortSubcommandsHelp(s: Scallop): String = {
      val maxCommandLength = s.subbuilders.map(_._1.size).max
      // todo: how to make this more generic?
      val filteredSubbuilders = s.subbuilders.filterNot(_._1 == "export")

      "\n\n" + getSubcommandsSectionName + "\n" +
      filteredSubbuilders.map { case (name, option) =>
        "  " + name.padTo(maxCommandLength, ' ') + "   " + option.descr
      }.mkString("\n")
    }
  }

  verify()
}
