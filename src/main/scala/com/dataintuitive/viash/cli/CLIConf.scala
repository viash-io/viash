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

package com.dataintuitive.viash.cli

import org.rogach.scallop.{ScallopConf, Subcommand}
import org.rogach.scallop.ScallopConfBase
import org.rogach.scallop.ScallopOptionGroup
import org.rogach.scallop.ValueConverter
import org.rogach.scallop.ScallopOption
import com.dataintuitive.viash.Main


trait ViashCommand {
  _: DocumentedSubcommand =>
  val platform = registerOpt[String](
    name = "platform",
    short = Some('p'),
    default = None,
    descr =
      "Specifies which platform amongst those specified in the config to use. " +
      "If this is not provided, the first platform will be used. " +
      "If no platforms are defined in the config, the native platform will be used. " +
      "In addition, the path to a platform yaml file can also be specified.",
    required = false
  )
  val config = registerTrailArg[String](
    name = "config",
    descr = "A viash config file (example: config.vsh.yaml). This argument can also be a script with the config as a header.",
    default = Some("config.vsh.yaml"),
    required = true
  )
  val config_mods = registerOpt[List[String]](
    name = "config_mod",
    short = Some('c'),
    default = Some(Nil),

    descr = "Modify a viash config at runtime using a custom DSL. For more information, see the online documentation."
  )
}
trait ViashNs {
  _: DocumentedSubcommand =>
  val query = registerOpt[String](
    name = "query",
    short = Some('q'),
    descr = "Filter which components get selected by name and namespace. Can be a regex. Example: \"^mynamespace/component1$\".",
    default = None
  )
  val query_namespace = registerOpt[String](
    name = "query_namespace",
    short = Some('n'),
    descr = "Filter which namespaces get selected by namespace. Can be a regex. Example: \"^mynamespace$\".",
    default = None
  )
  val query_name = registerOpt[String](
    name = "query_name",
    descr = "Filter which components get selected by name. Can be a regex. Example: \"^component1\".",
    default = None
  )
  val src = registerOpt[String](
    name = "src",
    short = Some('s'),
    descr = " A source directory containing viash config files, possibly structured in a hierarchical folder structure. Default: src/.",
    default = Some("src")
  )
  val platform = registerOpt[String](
    name = "platform",
    short = Some('p'),
    descr =
      "Acts as a regular expression to filter the platform ids specified in the found config files. " +
        "If this is not provided, all platforms will be used. " +
        "If no platforms are defined in a config, the native platform will be used. " +
        "In addition, the path to a platform yaml file can also be specified.",
    default = None,
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
    descr = "Modify a viash config at runtime using a custom DSL. For more information, see the online documentation."
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

class CLIConf(arguments: Seq[String]) extends ScallopConf(arguments) {
  def getRegisteredCommands = subconfigs.flatMap{ sc =>
    sc match {
      case ds: DocumentedSubcommand => Some(ds.toRegisteredCommand)
      case _ => None
    }
  }
 
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

  val run = new DocumentedSubcommand("run") with ViashCommand with WithTemporary {
    banner(
      s"""viash run
         |Executes a viash component from the provided viash config file. viash generates a temporary executable and immediately executes it with the given parameters.
         |
         |Usage:
         |  viash run config.vsh.yaml [-p docker] [-k true/false]  -- [arguments for script]
         |
         |Arguments:""".stripMargin)

    footer(
      s"""  -- param1 param2 ...    Extra parameters to be passed to the component itself.
         |                          -- is used to separate viash arguments from the arguments
         |                          of the component.
         |
         |The temporary directory can be altered by setting the VIASH_TEMP directory. Example:
         |  export VIASH_TEMP=/home/myuser/.viash_temp
         |  viash run config.vsh.yaml""".stripMargin)
  }

  val build = new DocumentedSubcommand("build") with ViashCommand {
    banner(
      s"""viash build
         |Build an executable from the provided viash config file.
         |
         |Usage:
         |  viash build config.vsh.yaml -o output [-p docker] [-m] [-s]
         |
         |Arguments:""".stripMargin)

    val printMeta = registerOpt[Boolean](
      name = "meta",
      short = Some('m'),
      default = Some(false),
      descr = "Print out some meta information at the end."
    )
    val writeMeta = registerOpt[Boolean](
      name = "write_meta",
      short = Some('w'),
      default = Some(false),
      descr = "Write out some meta information to RESOURCES_DIR/viash.yaml at the end."
    )
    val output = registerOpt[String](
      name = "output",
      descr = "Path to directory in which the executable and any resources is built to. Default: \"output/\".",
      default = Some("output/"),
      required = true
    )
    val setup = registerOpt[String](
      name = "setup",
      default = None,
      descr = "Which setup strategy for creating the container to use [Docker Platform only]."
    )
    val push = registerOpt[Boolean](
      name = "push",
      default = Some(false),
      descr = "Whether or not to push the container to a Docker registry [Docker Platform only]."
    )
  }

  val test = new DocumentedSubcommand("test") with ViashCommand with WithTemporary {
    banner(
      s"""viash test
         |Test the component using the tests defined in the viash config file.
         |
         |Usage:
         |  viash test config.vsh.yaml [-p docker] [-k true/false]
         |
         |Arguments:""".stripMargin)

    footer(
      s"""
         |The temporary directory can be altered by setting the VIASH_TEMP directory. Example:
         |  export VIASH_TEMP=/home/myuser/.viash_temp
         |  viash run meta.vsh.yaml""".stripMargin)
  }

  val config = new DocumentedSubcommand("config") {
    val view = new DocumentedSubcommand("view") with ViashCommand {
      banner(
        s"""viash config view
           |View the config file after parsing.
           |
           |Usage:
           |  viash config view config.vsh.yaml
           |
           |Arguments:""".stripMargin)
      val format = registerChoice(
        name = "format",
        short = Some('f'),
        default = Some("yaml"),
        choices = List("yaml", "json"),
        descr = "Which output format to use."
      )
    }
    val inject = new DocumentedSubcommand("inject") with ViashCommand {
      banner(
        s"""viash config inject
           |Inject a Viash header into the main script of a Viash component.
           |
           |Usage:
           |  viash config inject config.vsh.yaml
           |
           |Arguments:""".stripMargin)
    }

    addSubcommand(view)
    addSubcommand(inject)
    requireSubcommand()

    shortSubcommandsHelp(true)
  }

  val namespace = new DocumentedSubcommand("ns") {

    val build = new DocumentedSubcommand("build") with ViashNs{
      banner(
        s"""viash ns build
           |Build a namespace from many viash config files.
           |
           |Usage:
           |  viash ns build [-n nmspc] [-s src] [-t target] [-p docker] [--setup] [---push] [--parallel] [--flatten]
           |
           |Arguments:""".stripMargin)
      val target = registerOpt[String](
        name = "target",
        short = Some('t'),
        descr = "A target directory to build the executables into. Default: target/.",
        default = Some("target")
      )
      val setup = registerOpt[String](
        name = "setup",
        default = None,
        descr = "Which setup strategy for creating the container to use [Docker Platform only]."
      )
      val push = registerOpt[Boolean](
        name = "push",
        default = Some(false),
        descr = "Whether or not to push the container to a Docker registry [Docker Platform only]."
      )
      val writeMeta = registerOpt[Boolean](
        name = "write_meta",
        short = Some('w'),
        default = Some(false),
        descr = "Write out some meta information to RESOURCES_DIR/viash.yaml at the end."
      )
      val flatten = registerOpt[Boolean](
        name = "flatten",
        short = Some('f'),
        default = Some(false),
        descr = "Flatten the target builds, handy for building one platform to a bin directory."
      )
    }

    val test = new DocumentedSubcommand("test") with ViashNs with WithTemporary {
      banner(
        s"""viash ns test
           |Test a namespace containing many viash config files.
           |
           |Usage:
           |  viash ns test [-n nmspc] [-s src] [-p docker] [--parallel] [--tsv file.tsv] [--append]
           |
           |Arguments:""".stripMargin)
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

    val list = new DocumentedSubcommand("list") with ViashNs {
      banner(
        s"""viash ns list
           |List a namespace containing many viash config files.
           |
           |Usage:
           |  viash ns list [-n nmspc] [-s src] [-p docker]
           |
           |Arguments:""".stripMargin)
      val format = registerChoice(
        name = "format",
        short = Some('f'),
        default = Some("yaml"),
        choices = List("yaml", "json"),
        descr = "Which output format to use."
      )
    }

    addSubcommand(build)
    addSubcommand(test)
    addSubcommand(list)
    requireSubcommand()

    shortSubcommandsHelp(true)
  }

  val cliexport = opt[Boolean](
    name = "cli_export",
    default = Some(false),
    descr = "Export CLI information to json to allow automated documentation generation",
    noshort = true,
    hidden = true
  )

  addSubcommand(run)
  addSubcommand(build)
  addSubcommand(test)
  addSubcommand(namespace)
  addSubcommand(config)

  shortSubcommandsHelp(true)

  verify()
}
