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

case class RegisteredOpt (
  name: String,
  short: Option[Char],
  descr: String,
  default: String,
  required: Boolean,
  argName: Option[String],
  hidden: Boolean,
  noshort: Option[Boolean],
  choices: Option[Seq[String]],
  `type`: String,
  optType: String,
) 

object RegisteredOpt {
  def opt(name: String,
    short: Char,
    descr: String,
    default: String,
    // validate: A => Boolean = (_:A),
    required: Boolean,
    argName: String,
    hidden: Boolean,
    noshort: Boolean,
    // group: ScallopOptionGroup,
    `type`: String
  ) = RegisteredOpt(name, Some(short), descr, default, required, Some(argName), hidden, Some(noshort), None, `type`, "opt")

  def choice(
    choices: Seq[String],
    name: String,
    short: Char,
    descr: String,
    default: String,
    required: Boolean,
    argName: String,
    hidden: Boolean,
    noshort: Boolean,
  ) = RegisteredOpt(name, Some(short), descr, default, required, Some(argName), hidden, Some(noshort), Some(choices), "String", "choice")

  def trailArgs(
    name: String,
    descr: String,
    required: Boolean,
    default: String,
    hidden: Boolean,
    `type`: String
  ) = RegisteredOpt(name, None, descr, default, required, None, hidden, None, None, `type`, "trailArgs")


}