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

package io.viash.languages

/**
 * Represents a programming language.
 */
trait Language {
  // The unique identifier for the programming language
  val id: String

  // A short, human-readable name for the programming language
  val name: String

  // The file extensions associated with the programming language
  val extensions: Seq[String]

  // The comment string used for single-line comments in the programming language
  val commentStr: String

  // The command(s) used to execute a script written in the programming language
  val executor: Seq[String]

  // The code to parse Viash param YAML files in the programming language
  val viashParseYamlCode: String
  
  // The code to parse Viash param JSON files in the programming language
  val viashParseJsonCode: String

  def scriptTypeId = id + "_script"
}
