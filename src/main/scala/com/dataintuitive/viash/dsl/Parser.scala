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

/* DSL examples

# een simpele set
.functionality.version := "0.3.0"

# een subset van een lijst aanpassen:
.platforms[.type == "docker"].container_registry := "itx-aiv"

# iets aan een lijst toevoegen:
.functionality.authors += { name: "Mr. T", role: "sponsor" }


*/

/* BNF notation

<command>    ::= <path> ":=" <json>
               | <path> "+=" <json>

<path>       ::= "$" | <path2>
<path2>      ::= ""
               | <path2> "." <identifier>
               | <path2> "[" <condition> "]"

<condition>  ::= <value> "==" <value>
               | <value> "!=" <value>
               | <condition> "&&" <condition>
               | <condition> "||" <condition>
               | "(" <condition> ")"
<value>      ::= <path> | <string> | <number> | <json>

<identifier> ::= <letter> (<letter> | <digit> | "_")
<string>    ::= "'" <text1> "'" | '"' <text2> '"'
<text1>      ::= "" | <character1> <text1>
<text2>      ::= '' | <character2> <text2>

<letter>     ::= [a-zA-Z]
<digit>      ::= [0-9]
<character1> ::= [^\"] | '\"' | "\"
<character2> ::= [^\'] | "\'" | "\"
<number>     ::= # todo, support whole and real numbers, also scientific notation

<json>       ::= <jarray> | <jobject>
<jvalue>     ::= <string> | <number> | <json>
<jarray>     ::= "[" (<json> ("," <json>)*))? "]"
<jobject>    ::= "{" (<jprop> ("," <jprop>)*))? "}"
<jprop>      ::= <jname> ":" <jvalue>
<jprop>      ::= <identifier> | <string>

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
