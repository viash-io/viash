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

import org.rogach.scallop.ScallopConfBase
import io.circe.{Printer => JsonPrinter}
import io.circe.syntax.EncoderOps
import com.dataintuitive.viash.helpers.Circe._
// import com.dataintuitive.viash.cli._
import com.dataintuitive.viash.cli.CLICommand

case class CLICommand (
  name: String,
  banner: Option[String],
  footer: Option[String],
  subcommands: Seq[CLICommand],
  opts: Seq[RegisteredOpt],
)

object CLICommand {
  implicit def fromScallopConfBase(scb: ScallopConfBase): Option[CLICommand] = scb match {
    case dc: DocumentedSubcommand => Option(fromDocumentedSubCommand(dc))
    case _ => None
  }
  implicit def fromDocumentedSubCommand(ds: DocumentedSubcommand): CLICommand = 
    CLICommand(
      ds.getCommandNameAndAliases.mkString(" + "),
      ds.getBanner,
      ds.getFooter,
      ds.registeredSubCommands.map(ds => fromDocumentedSubCommand(ds)),
      ds.registeredOpts,
    )
}

object CLIExport {

  private val jsonPrinter = JsonPrinter.spaces2.copy(dropNullValues = true)

  def export() {
    val cli = new CLIConf(Nil)
    val data = cli.getSubconfigs.flatMap(CLICommand.fromScallopConfBase)
    val str = jsonPrinter.print(data(0).asJson)
    println(str)
  }
}
