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

package com.dataintuitive.viash.dsl

import io.circe.Json

import scala.util.parsing.combinator.RegexParsers

/* examples
.functionality.version := "0.3.0"
.platforms[.type == "docker"].container_registry := "itx-aiv"
.functionality.authors += { name: "Mr. T", role: "sponsor" }

[type == "foo"]
*/

/* BNF notation

<block>      ::= <command> ("[\n;]" <command>)*

<command>    ::= <path> <whitespace> ":=" <whitespace> <json>
               | <path> <whitespace> "+=" <whitespace> <json>

<path>       ::= <identifier>
               | <path> "." <identifier>
               | <path> "[" <whitespace> <condition> <whitespace> "]"

<condition>  ::= <value> <whitespace> "==" <whitespace> <value<
<value>      ::= <path> | <literal>

<identifier> ::= <letter> (<letter> | <digit> | "_")
<literal>    ::= "'" <text1> "'" | '"' <text2> '"'
<text>       ::= "" | <character> <text>
<text1>      ::= "" | <character1> <text1>
<text2>      ::= '' | <character2> <text2>

<letter>     ::= "A" | "B" | "C" | "D" | "E" | "F" | "G" | "H" | "I" | "J" | "K" | "L" | "M" | "N" | "O" | "P" | "Q" | "R" | "S" | "T" | "U" | "V" | "W" | "X" | "Y" | "Z" | "a" | "b" | "c" | "d" | "e" | "f" | "g" | "h" | "i" | "j" | "k" | "l" | "m" | "n" | "o" | "p" | "q" | "r" | "s" | "t" | "u" | "v" | "w" | "x" | "y" | "z"
<digit>      ::= "0" | "1" | "2" | "3" | "4" | "5" | "6" | "7" | "8" | "9"
<symbol>     ::=  "|" | " " | "!" | "#" | "$" | "%" | "&" | "(" | ")" | "*" | "+" | "," | "-" | "." | "/" | ":" | ";" | ">" | "=" | "<" | "?" | "@" | "[" | "\" | "]" | "^" | "_" | "`" | "{" | "}" | "~"
<character>  ::= <letter> | <digit> | <symbol>
<character1> ::= <character> | "'"
<character2> ::= <character> | '"'

<whitespace> ::= " " <whitespace> | ""

*/


object CommandLexer extends RegexParsers {
  override def skipWhitespace = true

  override val whiteSpace = "[ \t\r\f]+".r

  // define values
  def identifier: Parser[String] = """[a-zA-Z][a-zA-Z0-9_]*""".r ^^ {
    _.toString
  }
  def literal: Parser[Literal] = {
    // TODO: can probably solve this nicer with something like "'" ~> "[^']" <~ "'"
    """"[^"]*"|'[^']*'""".r ^^ { str =>
      val content = str.substring(1, str.length - 1)
      Literal(content)
    }
  }
  def json: Parser[Json] = {
    // TODO: I should use a better parser for this to make sure the brackets match etc.
    ".*".r ^^ { str =>
      io.circe.yaml.parser.parse(str).toOption.get // TODO: will fail when str is not valid json/yaml
      // str.asJson
    }
  }

  // define paths
  def root: Parser[Path] = "$" ^^ { _ => Path(Nil)}
  def pathList: Parser[Path] = rep1(down | filter) ^^ { Path(_) }
  def path: Parser[Path] = root | pathList
  def down: Parser[PathExp] = "." ~> identifier ^^ { id => Attribute(id) }
  def filter: Parser[PathExp] = "[" ~> condition <~ "]" ^^ { con => Filter(con) }

  // define condition operations
  def condition: Parser[Condition] = equals | cTrue | cFalse
  def cTrue: Parser[Condition] = "true" ^^ { _ => True }
  def cFalse: Parser[Condition] = "false" ^^ { _ => False }

  def equals: Parser[Equals] = value ~ "==" ~ value ^^ {
    case left ~ _ ~ right => Equals(left, right)
  }
  def and: Parser[And] = condition ~ "&&" ~ condition ^^ {
    case left ~ _ ~ right => And(left, right)
  }
  def or: Parser[Or] = condition ~ "||" ~ condition ^^ {
    case left ~ _ ~ right => Or(left, right)
  }
  def not: Parser[Not] = "!" ~> condition ^^ { Not(_) }

  // define condition values
  def value: Parser[Value] = literal | path

  // define commands
  def modify: Parser[Modify] = path ~ ":=" ~ json ^^ {
    case pt ~ _ ~ js => Modify(pt, js)
  }
  def add: Parser[Add] = path ~ "+=" ~ json ^^ {
    case pt ~ _ ~ js => Add(pt, js)
  }
  def command: Parser[Command] = modify | add
}
