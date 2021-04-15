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

package com.dataintuitive.viash.config_mods

import io.circe.Json

import scala.util.parsing.combinator.RegexParsers
import io.circe.syntax._


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

<path>       ::= "root" | <path2>
<path2>      ::= "." <identifier> <path2>?
               | "[" <condition> "]" <path2>?

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


object ConfigModParser extends RegexParsers {
  def parseBlock(s: String): ConfigMods = {
    parse(block, s).get
    // TODO: provide better error message
  }

  override def skipWhitespace = true

  override val whiteSpace = "[ \t\r\f]+".r

  // basic types
  def whole: Parser[Integer] = """[+-]?[0-9]+""".r ^^ { _.toInt }
  def real: Parser[Double] = """[+-]?[0-9]+(((\.[0-9]+)?[eE][+-]?[0-9]+[Ff]?)|(\.[0-9]+[Ff]?)|([Ff]))""".r ^^ { _.toDouble }
  def string: Parser[String] = """"([^"]|\\")*"|'([^']|\\')*'""".r ^^ { str =>
    val quoteChar = str.substring(0, 1)
    str.substring(1, str.length - 1).replaceAll("\\" + quoteChar, quoteChar)
  }
  def boolean: Parser[Boolean] = "true" ^^^ true | "false" ^^^ false

  def identifier: Parser[String] = """[a-zA-Z][a-zA-Z0-9_]*""".r

  // json parsers
  def json: Parser[Json] = objJson | arrayJson | realJson | wholeJson | stringJson | booleanJson
  def objJson: Parser[Json] = "{" ~> repsep(fieldJson, ",") <~ "}" ^^ { Json.fromFields(_) }
  def arrayJson: Parser[Json] = "[" ~> repsep(json, ",") <~ "]" ^^ { Json.fromValues(_) }
  def fieldJson: Parser[(String, Json)] = (identifier | string) ~ ( ":" ~> json) ^^ {
    case id ~ va => (id, va)
  }
  def wholeJson: Parser[Json] = whole ^^ { _.asJson }
  def realJson: Parser[Json] = real ^^ { _.asJson }
  def stringJson: Parser[Json] = string ^^ { _.asJson }
  def booleanJson: Parser[Json] = boolean ^^ { _.asJson }
  def nullJson: Parser[Json] = "null" ^^^ None.asJson


  // define paths
  def root: Parser[PathExp] = "root" ^^ { _ => Root}
  def path: Parser[Path] = (root | down | filter) ~ rep(down | filter) ^^ {
    case head ~ tail => {
      Path(head :: tail)
    }
  }
  def down: Parser[PathExp] = "." ~> identifier ^^ { Attribute(_) }
  def filter: Parser[PathExp] = "[" ~> condition <~ "]" ^^ { Filter(_) }

  // define condition operations
  def condition: Parser[Condition] = and | or | not | condAvoidRecursion
  def condAvoidRecursion: Parser[Condition] = brackets | equals | notEquals | ("true" ^^^ True) | ("false" ^^^ False)
  def brackets: Parser[Condition] = "(" ~> condition <~ ")"
  def and: Parser[And] = condAvoidRecursion ~ ("&&" ~> condition) ^^ {
    case left ~  right => And(left, right)
  }
  def or: Parser[Or] = condAvoidRecursion ~ ("||" ~> condition) ^^ {
    case left ~ right => Or(left, right)
  }
  def not: Parser[Not] = "!" ~> condition ^^ { Not(_) }

  def equals: Parser[Equals] = value ~ ("==" ~> value) ^^ {
    case left ~ right => Equals(left, right)
  }

  def notEquals: Parser[NotEquals] = value ~ ("!=" ~> value) ^^ {
    case left ~ right => NotEquals(left, right)
  }

  // define condition values
  def value: Parser[Value] = path | (json ^^ { JsonValue(_) })

  // define commands
  def command: Parser[ConfigMod] = path ~ (modify | add) ^^ {
    case pt ~ comm => ConfigMod(pt, comm)
  }
  def modify: Parser[CommandExp] = ":=" ~> json ^^ { Modify(_) }
  def add: Parser[CommandExp] = "+=" ~> json ^^ { Add(_) }
  def block: Parser[ConfigMods] = repsep(command, ";") ^^ { ConfigMods(_) }
}
