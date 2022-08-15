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

package io.viash.config_mods

import io.circe.Json

import scala.util.parsing.combinator.RegexParsers
import io.circe.syntax._


/* DSL examples

# setting a value
.functionality.version := "0.3.0"

# setting a value after subsetting a list
.platforms[.type == "docker"].container_registry := "itx-aiv"

# add something to a list
.functionality.authors += { name: "Mr. T", role: "sponsor" }

# prepend something to a list
.functionality.authors +0= { name: "Mr. T", role: "sponsor" }

# apply config mod before parsing the json
<preparse> .platforms[.type == "nextflow"].variant := "vdsl3"

# delete a value
del(.functionality.version)

# check an identifier is specified
<preparse> .functionality.authors[!has(roles)].email := "unknown"

*/

/* BNF notation

<command>    ::= <path> ":=" <json> | <path>
               | <path> "+0=" <json>
               | "del(" <path> ")"
               | <path> "+=" <json>
               | "<preparse>" <command>

<path>       ::= "root" | <path2>
<path2>      ::= "." <identifier> <path2>?
               | "[" <condition> "]" <path2>?

<condition>  ::= <value> "==" <value>
               | <value> "!=" <value>
               | "has(" <path> ")"
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
  implicit class RichParser[A](p: Parser[A]) {
    def parse(s: String): A = {
      ConfigModParser.parse(p, s) match {
        case Success(result, next) => result
        case _: NoSuccess => 
          throw new IllegalArgumentException("Cound not parse config mod: " + s)
      }
    }
  }

  def parseBlock(s: String): ConfigMods = {
    val configMods = block.parse(s)
    if (s != "" && configMods.preparseCommands.isEmpty && configMods.postparseCommands.isEmpty) {
      throw new RuntimeException("Could not parse config mods: '" + s + "'")
    }
    configMods
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
  def json: Parser[Json] = objJson | arrayJson | realJson | wholeJson | stringJson | booleanJson | nullJson
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
  def root: Parser[PathExp] = "root" ^^^ Root
  def path: Parser[Path] = (root | down | filter) ~ rep(down | filter) ^^ {
    case head ~ tail => {
      Path(head :: tail)
    }
  }
  def down: Parser[PathExp] = "." ~> identifier ^^ { Attribute(_) }
  def filter: Parser[PathExp] = "[" ~> condition <~ "]" ^^ { Filter(_) }

  // define condition operations
  def condition: Parser[Condition] = and | or | not | condAvoidRecursion
  def condAvoidRecursion: Parser[Condition] = brackets | equals | notEquals | has | ("true" ^^^ True) | ("false" ^^^ False)
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

  def has: Parser[Has] = "has(" ~> path <~ ")" ^^ { Has(_) }

  // define condition values
  def value: Parser[Value] = path | (json ^^ { JsonValue(_) })

  // define commands
  def command: Parser[(Boolean, Command)] = preparse ~ (delete | assign | append | prepend) ^^ {
    case prep ~ cm => (prep, cm)
  }
  def delete: Parser[Command] = "del(" ~> path <~ ")" ^^ { pt => 
    Delete(pt)
  }
  def assign: Parser[Command] = path ~ (":=" ~> value) ^^ { 
    case lhs ~ rhs => Assign(lhs, rhs)
  }
  def append: Parser[Command] = path ~ ("+=" ~> value) ^^ { 
    case lhs ~ rhs => Append(lhs, rhs)
  }
  def prepend: Parser[Command] = path ~ ("+0=" ~> value) ^^ { 
    case lhs ~ rhs => Prepend(lhs, rhs)
  }
  def block: Parser[ConfigMods] = repsep(command, ";") ^^ { cmds =>
    val preparseCommands = cmds.filter(_._1).map(_._2)
    val postparseCommands = cmds.filter(!_._1).map(_._2)
    ConfigMods(
      postparseCommands = postparseCommands,
      preparseCommands = preparseCommands
    )
  }
  def preparse: Parser[Boolean] = opt("<preparse>") ^^ {
    case found => found.isDefined
  }
}
