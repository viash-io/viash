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

/**
  * Wrapper class for Subcommand to expose protected members
  * We need this information to scrape the CLI to export to json
  */
class DocumentedSubcommand(commandNameAndAliases: String*) extends Subcommand(commandNameAndAliases:_*) {
  def getCommandNameAndAliases = commandNameAndAliases
  def getBanner = builder.bann
  def getFooter = builder.foot
  // def getOpts = builder.opts
  // def getSubconfigs = subconfigs
  var registeredSubCommands: Seq[DocumentedSubcommand] = Nil
  var registeredOpts: Seq[RegisteredOpt] = Nil

  import scala.reflect.runtime.universe._

  override def addSubcommand(conf: Subcommand): Unit = {
    if (conf.isInstanceOf[DocumentedSubcommand])
      registeredSubCommands = registeredSubCommands :+ conf.asInstanceOf[DocumentedSubcommand]
    super.addSubcommand(conf)
  }

  // We need to get the TypeTag[A], which changes the interface of 'opt', however since we have default values we can't just overload the methods.
  // The same goes for 'trailArgs'. Not really for 'choice' but it's better to keep the same change in naming schema here too.

  def registerOpt[A](
    name: String = null,
    short: Char = '\u0000',
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
    
    registeredOpts = registeredOpts :+ RegisteredOpt.opt(cleanName, short, descr, default.toString(), required, argName, hidden, noshort, `type`.toString())
    opt(name, short, descr, default, validate, required, argName, hidden, noshort, group)
  }

  def registerChoice(
    choices: Seq[String],
    name: String = null,
    short: Char = '\u0000',
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

    registeredOpts = registeredOpts :+ RegisteredOpt.choice(choices, cleanName, short, descr, default.toString(), required, argName, hidden, noshort)
    choice(choices, name, short, descr, default, required, argName, hidden, noshort, group)
  }

  def registerTrailArg[A](
    name: String = null,
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

    registeredOpts = registeredOpts :+ RegisteredOpt.trailArgs(cleanName, descr, required, default.toString(), hidden, `type`.toString())
    trailArg[A](name, descr, validate, required, default, hidden, group)
  }

}