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

package com.dataintuitive.viash

import org.rogach.scallop.ScallopConfBase
import io.circe.{Json, Printer => JsonPrinter}
import org.rogach.scallop.Scallop
import io.circe.Encoder
import io.circe.syntax.EncoderOps

import com.dataintuitive.viash.helpers.Circe._
import io.circe.generic.extras.semiauto.deriveConfiguredEncoder
import com.dataintuitive.viash.cli._
import org.rogach.scallop.CliOption
import com.dataintuitive.viash.CLIOption.fromCliOption
import com.dataintuitive.viash.CLICommand.fromDocumentedSubCommand

case class CLICommand (
  name: String,
  banner: Option[String],
  footer: Option[String],
  subcommands: Seq[CLICommand],
  opts: Seq[CLIOption]
)

case class CLIOption (
  name: String,
  shortNames: Seq[Char],
  descr: String,
  default: Option[String]
)

object CLICommand {
  implicit def fromDocumentedSubCommand(ds: DocumentedSubcommand): CLICommand = 
    CLICommand(
      ds.getCommandNameAndAliases.mkString(" + "),
      ds.getBanner,
      ds.getFooter,
      ds.getSubconfigs.flatMap(_ match {
        case ds2: DocumentedSubcommand => Option(fromDocumentedSubCommand(ds2))
        case _ => None
      }),
      ds.getOpts.map(o => fromCliOption(o))
    )
}

object CLIOption {
  implicit def fromCliOption(o: CliOption): CLIOption = CLIOption(o.name, o.shortNames, o.descr, o.default().map(d => d.toString()))
}

object CLIExport {

  def printInformation(subconfigs: Seq[ScallopConfBase]) {
    subconfigs.map(
      _ match {
        case ds: DocumentedSubcommand => printInformation(ds)
        case u => Console.err.println(s"CLIExport: Unsupported type $u")
      }
    )
  }

  def printInformation(command: DocumentedSubcommand) {
    println(s"command name: ${command.getCommandNameAndAliases.mkString(" + ")}")
    println(s"\tbanner: ${command.getBanner}")
    println(s"\tfooter: ${command.getFooter}")
    for (o <- command.getOpts) {
      println(s"\n\tname: ${o.name} short: ${o.shortNames.mkString(",")} descr: ${o.descr} default: ${o.default()}")
    }
    for (c <- command.getSubconfigs) {
      c match {
        case ds: DocumentedSubcommand => printInformation(ds)
        case _ => println(s"Unsupported type ${c.toString}")
      }
    }
  }

  private val jsonPrinter = JsonPrinter.spaces2.copy(dropNullValues = true)

  def export() {
    val cli = new CLIConf(Nil)
    val data = cli.getSubconfigs.flatMap(_ match {
      case ds: DocumentedSubcommand => Option(fromDocumentedSubCommand(ds))
      case _ => None
    })
    val str = jsonPrinter.print(data.asJson)
    println(str)

    // printInformation(cli.getSubconfigs)
  }
}
