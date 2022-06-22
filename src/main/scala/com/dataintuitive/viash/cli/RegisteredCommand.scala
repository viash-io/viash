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

case class RegisteredCommand (
  name: String,
  banner: Option[String],
  footer: Option[String],
  subcommands: Seq[RegisteredCommand],
  opts: Seq[RegisteredOpt],
)

object RegisteredCommand {
  def maybeWrap(scb: ScallopConfBase): Option[RegisteredCommand] = scb match {
    case dc: DocumentedSubcommand => Option(wrap(dc))
    case _ => None
  }
  def wrap(ds: DocumentedSubcommand): RegisteredCommand = 
    RegisteredCommand(
      name = ds.getCommandNameAndAliases.mkString(" + "),
      banner = ds.getBanner,
      footer = ds.getFooter,
      subcommands = ds.registeredSubCommands.map(ds => wrap(ds)),
      opts = ds.registeredOpts,
    )
}