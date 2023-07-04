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

object AutoComplete {

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
  
  def generateForBash(cli: CLIConf): String = {

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
