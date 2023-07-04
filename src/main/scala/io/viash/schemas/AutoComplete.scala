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

import io.viash.cli.CLIConf
import io.viash.cli.DocumentedSubcommand
import cats.syntax.nested
import io.viash.cli.RegisteredCommand

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
    val cmdStr = cmd.subcommands
      .map(subCmd => commandArguments(subCmd))
      .map(s => s.replace("\n", "\n|    "))
    
    val cmdName = cmd.name

    s"""_viash_$cmdName()
       |  case $${words[2]} in
       |    ${cmdStr.mkString("\n|    ")}
       |  esac
       |}
       |""".stripMargin

  }
  
  def generateForBash(cli: CLIConf): String = {

    val (commands, nestedCommands) = cli.getRegisteredCommands(true).partition(_.subcommands.isEmpty)

    for(c <- commands) {
      println(s"command: ${commandArguments(c)}")
    }

    for(nc <- nestedCommands) {
      println(s"nestedCommands: ${nestedCommand(nc)}")
    }
   
    ""
  }


}
