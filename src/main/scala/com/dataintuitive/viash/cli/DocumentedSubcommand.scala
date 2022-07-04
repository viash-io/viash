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

import org.rogach.scallop.Subcommand
import org.rogach.scallop.ScallopOptionGroup
import org.rogach.scallop.ValueConverter
import org.rogach.scallop.ScallopOption
import scala.reflect.runtime.universe._

/**
  * Wrapper class for Subcommand to expose protected members
  * We need this information to scrape the CLI to export to json
  */
class DocumentedSubcommand(commandNameAndAliases: String*) extends Subcommand(commandNameAndAliases:_*) {
  var registeredSubCommands: Seq[DocumentedSubcommand] = Nil
  var registeredOpts: Seq[RegisteredOpt] = Nil


  override def addSubcommand(conf: Subcommand): Unit = {
    if (conf.isInstanceOf[DocumentedSubcommand]) {
      registeredSubCommands = registeredSubCommands :+ conf.asInstanceOf[DocumentedSubcommand]
    }
    super.addSubcommand(conf)
  }

  // We need to get the TypeTag[A], which changes the interface of 'opt', however since we have default values we can't just overload the methods.
  // The same goes for 'trailArgs'. Not really for 'choice' but it's better to keep the same change in naming schema here too.

  def registerOpt[A](
    name: String,
    short: Option[Char] = None,
    descr: String = "",
    default: => Option[A] = None,
    validate: A => Boolean = (_:A) => true,
    required: Boolean = false,
    argName: String = "arg",
    hidden: Boolean = false,
    noshort: Boolean = builder.noshort,
    group: ScallopOptionGroup = null
  )(implicit conv:ValueConverter[A], tag: TypeTag[A]): ScallopOption[A] = {

    val `type` = tag.tpe
    val cleanName = name match {
      case null => ""
      case _ => name
    }
    
    val registeredOpt = RegisteredOpt(
      name = cleanName, 
      short = short,
      descr = descr, 
      default = default.map(_.toString()), 
      required = required, 
      argName = Some(argName), 
      hidden = hidden, 
      noshort = Some(noshort), 
      choices = None, 
      `type` = `type`.toString(), 
      optType = "opt"
    )
    registeredOpts = registeredOpts :+ registeredOpt
    opt(name, short.getOrElse('\u0000'), descr, default, validate, required, argName, hidden, noshort, group)
  }

  def registerChoice(
    choices: Seq[String],
    name: String,
    short: Option[Char],
    descr: String = "",
    default: => Option[String] = None,
    required: Boolean = false,
    argName: String = "arg",
    hidden: Boolean = false,
    noshort: Boolean = noshort,
    group: ScallopOptionGroup = null
  ): ScallopOption[String] = {

    val cleanName = name match {
      case null => ""
      case _ => name
    }

    val registeredOpt = RegisteredOpt(
      name = cleanName, 
      short = short, 
      descr = descr, 
      default = default.map(_.toString()), 
      required = required, 
      argName = Some(argName), 
      hidden = hidden, 
      noshort = Some(noshort), 
      choices = Some(choices), 
      `type` = "String", 
      optType = "choice"
    )
    registeredOpts = registeredOpts :+ registeredOpt
    choice(choices, name, short.getOrElse('\u0000'), descr, default, required, argName, hidden, noshort, group)
  }

  def registerTrailArg[A](
    name: String,
    descr: String = "",
    validate: A => Boolean = (_:A) => true,
    required: Boolean = true,
    default: => Option[A] = None,
    hidden: Boolean = false,
    group: ScallopOptionGroup = null
  )(implicit conv:ValueConverter[A], tag: TypeTag[A]) = {

    val `type` = tag.tpe
    val cleanName = name match {
      case null => ""
      case _ => name
    }

    val registeredOpt = RegisteredOpt(
      name = cleanName, 
      short = None, 
      descr = descr, 
      default = default.map(_.toString()), 
      required = required, 
      argName = None, 
      hidden = hidden, 
      noshort = None, 
      choices = None, 
      `type` = `type`.toString, 
      optType = "trailArgs"
    )
    registeredOpts = registeredOpts :+ registeredOpt
    trailArg[A](name, descr, validate, required, default, hidden, group)
  }

  def toRegisteredCommand: RegisteredCommand = 
    RegisteredCommand(
      name = commandNameAndAliases.mkString(" + "),
      banner = builder.bann,
      footer = builder.foot,
      subcommands = registeredSubCommands.map(_.toRegisteredCommand),
      opts = registeredOpts
    )
}