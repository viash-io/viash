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

package io.viash.schemas

import io.viash.cli._

object AutoCompleteBash {
  def commandArguments(cmd: RegisteredCommand): String = {
    val (opts, trailOpts) = cmd.opts.partition(_.optType != "trailArgs")
    val optNames = opts.map(_.name) ++ Seq("help")
    val cmdName = cmd.name

    trailOpts match {
      case Nil =>
        s"""$cmdName)
           |  COMPREPLY=($$(compgen -W ${optNames.mkString("'--", " --", "'")} -- "$$cur"))
           |  return
           |  ;;
           |""".stripMargin
      case _ =>
        s"""$cmdName)
           |  if [[ $$cur == -* ]]; then
           |    COMPREPLY=($$(compgen -W ${optNames.mkString("'--", " --", "'")} -- "$$cur"))
           |    return
           |  fi
           |  _filedir
           |  ;;
           |""".stripMargin
    }

  }
  def nestedCommand(cmd: RegisteredCommand): String = {
    val cmdStr = cmd.subcommands.map(subCmd => commandArguments(subCmd))
    val cmdName = cmd.name

    s"""_viash_$cmdName()
       |{
       |  case $${words[2]} in
       |    ${cmdStr.flatMap(s => s.split("\n")).mkString("\n|    ")}
       |  esac
       |}
       |""".stripMargin

  }
  
  def generate(cli: CLIConf): String = {

    val (commands, nestedCommands) = cli.getRegisteredCommands(true).partition(_.subcommands.isEmpty)

    val topLevelCommandNames = cli.getRegisteredCommands().map(_.name)

    val nestedCommandsSwitch = nestedCommands.map{nc =>
      val ncn = nc.name
      s"""$ncn)
         |  _viash_$ncn
         |  return
         |  ;;
         |""".stripMargin
    }

    val nestedCommandsSwitch2 = nestedCommands.map{nc =>
      val ncn = nc.name
      val subcommands = nc.subcommands.map(_.name)
      s"""$ncn)
         |  COMPREPLY=($$(compgen -W '${subcommands.mkString(" ")}' -- "$$cur"))
         |  return
         |  ;;
         |""".stripMargin
    }

    s"""# bash completion for viash
       |
       |${nestedCommands.map(nc => nestedCommand(nc)).mkString("\n")}
       |_viash()
       |{
       |  local cur prev words cword
       |  _init_completion || return
       |  if [[ $$cword -ge 3 ]]; then
       |    case $${words[1]} in
       |      ${nestedCommandsSwitch.flatMap(_.split("\n")).mkString("\n|      ")}
       |    esac
       |  fi
       |
       |  case $$prev in
       |    --version | --help | -!(-*)[hV])
       |      return
       |      ;;
       |    ${commands.flatMap(c => commandArguments(c).split("\n")).mkString("\n|    ")}
       |    ${nestedCommandsSwitch2.flatMap(_.split("\n")).mkString("\n|    ")}
       |  esac
       |
       |  if [[ $$cur == -* ]]; then
       |    COMPREPLY=($$(compgen -W '-h -v' -- "$$cur"))
       |  elif [[ $$cword == 1 ]]; then
       |    COMPREPLY=($$(compgen -W '${topLevelCommandNames.mkString(" ")}' -- "$$cur"))
       |  fi
       |
       |} &&
       |  complete -F _viash viash
       |""".stripMargin
  }
}

object AutoCompleteZsh {
  def commandArguments(cmd: RegisteredCommand): String = {
    def removeMarkup(text: String): String = {
      val markupRegex = raw"@\[(.*?)\]\(.*?\)".r
      val backtickRegex = "`(\"[^`\"]*?\")`".r
      val textWithoutMarkup = markupRegex.replaceAllIn(text, "$1")
      backtickRegex.replaceAllIn(textWithoutMarkup, "$1")
    }
    def getCleanDescr(opt: RegisteredOpt): String = {
      removeMarkup(opt.descr)
        .replaceAll("([\\[\\]\"])", "\\\\$1") // escape square brackets and quotes
    }

    val (opts, trailOpts) = cmd.opts.partition(_.optType != "trailArgs")
    val cmdArgs = opts.map(o => 
      if (o.short.isEmpty) {
        s""""--${o.name}[${getCleanDescr(o)}]""""
      } else {
        s""""(-${o.short.get} --${o.name})"{-${o.short.get},--${o.name}}"[${getCleanDescr(o)}]""""
      }
    )
    val cmdName = cmd.name

    trailOpts match {
      case Nil =>
        s"""$cmdName)
            |  local -a cmd_args
            |  cmd_args=(
            |    ${cmdArgs.mkString("\n|    ")}
            |  )
            |  _arguments $$cmd_args $$_viash_help $$_viash_id_comp
            |  ;;
            |""".stripMargin
      case _ =>
        s"""$cmdName)
            |  if [[ $${lastParam} == -* ]]; then
            |    local -a cmd_args
            |    cmd_args=(
            |      ${cmdArgs.mkString("\n|      ")}
            |    )
            |    _arguments $$cmd_args $$_viash_help $$_viash_id_comp
            |  else
            |    _files
            |  fi
            |  ;;
            |""".stripMargin
    }
  }


  def nestedCommand(cmd: RegisteredCommand): String = {
    val cmdStr = cmd.subcommands.map(subCmd => commandArguments(subCmd))
    val cmdName = cmd.name
    val subCmds = cmd.subcommands.map(subCmd => s""""${subCmd.name}:${subCmd.bannerDescription.get.split("\n").head}"""")

    s"""_viash_${cmdName}_commands() {
       |  local -a ${cmdName}_commands
       |  local lastParam
       |  lastParam=$${words[-1]}
       |
       |  ${cmdName}_commands=(
       |    ${subCmds.mkString("\n|    ")}
       |  )
       |
       |  if [[ CURRENT -eq 3 ]]; then
       |    if [[ $${lastParam} == -* ]]; then
       |      _arguments $$_viash_help $$_viash_id_comp
       |    else
       |      _describe -t commands "viash subcommands" ${cmdName}_commands
       |    fi
       |  else
       |    case $${words[3]} in
       |      ${cmdStr.flatMap(s => s.split("\n")).mkString("\n|      ")}
       |    esac
       |  fi
       |}
       |""".stripMargin
  }

  def generate(cli: CLIConf) = {

    val (commands, nestedCommands) = cli.getRegisteredCommands(true).partition(_.subcommands.isEmpty)

    val topLevelCommandNames = cli.getRegisteredCommands()

    val topCmds = topLevelCommandNames.map(subCmd => s""""${subCmd.name}:${subCmd.bannerDescription.getOrElse(s"${subCmd.name} operations subcommand").split("\n").head}"""")

    val nestedCommandsSwitch = nestedCommands.map{nc =>
      s"""${nc.name})
         |  _viash_${nc.name}_commands
         |  ;;
         |""".stripMargin
    }


    s"""#compdef viash
       |
       |local -a _viash_id_comp
       |_viash_id_comp=('1: :->id_comp')
       |
       |local -a _viash_help
       |_viash_help=('(-h --help)'{-h,--help}'[Show help message]')
       |
       |_viash_top_commands() {
       |  local -a top_commands
       |  top_commands=(
       |    ${topCmds.mkString("\n|    ")}
       |  )
       |
       |  _arguments \\
       |    '(-v --version)'{-v,--version}'[Show verson of this program]' \\
       |    $$_viash_help \\
       |    $$_viash_id_comp
       |
       |  _describe -t commands "viash subcommands" top_commands
       |}
       |
       |${nestedCommands.map(nc => nestedCommand(nc)).mkString("\n")}
       |
       |_viash() {
       |  local lastParam
       |  lastParam=$${words[-1]}
       |
       |  if [[ CURRENT -eq 2 ]]; then
       |    _viash_top_commands
       |  elif [[ CURRENT -ge 3 ]]; then
       |    case "$$words[2]" in
       |      ${commands.flatMap(c => commandArguments(c).split("\n")).mkString("\n|      ")}
       |      ${nestedCommandsSwitch.flatMap(_.split("\n")).mkString("\n|      ")}
       |    esac
       |  fi
       |
       |  return
       |}
       |
       |_viash
       |
       |# ex: filetype=sh
       |""".stripMargin
  }
}
