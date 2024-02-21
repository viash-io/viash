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
.version := "0.3.0"

# setting a value after subsetting a list
.platforms[.type == "docker"].container_registry := "itx-aiv"

# add something to a list
.authors += { name: "Mr. T", role: "sponsor" }

# prepend something to a list
.authors +0= { name: "Mr. T", role: "sponsor" }

# apply config mod before parsing the json
<preparse> .platforms[.type == "nextflow"].variant := "vdsl3"

# delete a value
del(.version)

# check an identifier is specified
<preparse> .authors[!has(roles)].email := "unknown"

*/

/* BNF notation

<command>    ::= <path> ":=" <value>
               | <path> "+0=" <value>
               | "del(" <path> ")"
               | <path> "+=" <value>
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


*/


object ConfigModParser extends RegexParsers {
  implicit class RichParser[A](p: Parser[A]) {
    def parse(s: String): A = {
      ConfigModParser.parseAll(p, s) match {
        case Success(result, next) => result
        case _: NoSuccess => 
          throw new IllegalArgumentException("Cound not parse config mod: " + s)
      }
    }
  }

  override def skipWhitespace = true

  override val whiteSpace = "[ \t\r\f]+".r

  /**
    * A collection of basic value parsers
    * 
    * Examples of 'whole':
    *   "42" => 42
    *   "-10" => -10
    * 
    * Examples of 'real': 
    *   "3.14" => 3.14
    *   "-1.5E-2" => -0.015
    * 
    * Examples of 'string': 
    *   "\"hello\"" => "hello"
    *   "'world'" => "world"
    * 
    * Examples 'boolean': 
    *   "true" => true
    *   "false" => false
    * 
    * Examples of 'identifier': 
    *   "myVariable" => "myVariable"
    *   "item_2" => "item_2"
    */
  def whole: Parser[Integer] = """[+-]?[0-9]+""".r ^^ { _.toInt }
  def real: Parser[Double] = """[+-]?[0-9]+(((\.[0-9]+)?[eE][+-]?[0-9]+[Ff]?)|(\.[0-9]+[Ff]?)|([Ff]))""".r ^^ { _.toDouble }
  def string: Parser[String] = """"([^"]|\\")*"|'([^']|\\')*'""".r ^^ { str =>
    val quoteChar = str.substring(0, 1)
    str.substring(1, str.length - 1).replaceAll("\\" + quoteChar, quoteChar)
  }
  def boolean: Parser[Boolean] = "true" ^^^ true | "false" ^^^ false
  def identifier: Parser[String] = """[a-zA-Z][a-zA-Z0-9_]*""".r

  /**
    * A collection of basic Json parsers
    * 
    * BNF notation:
    *   <json> ::= <objJson> | <arrayJson> | <realJson> | <wholeJson> | <stringJson> | <booleanJson> | <nullJson>
    *   <objJson> ::= "{" <fieldJsonList> "}"
    *   <arrayJson> ::= "[" <jsonList> "]"
    *   <fieldJson> ::= (<identifier> | <string>) ":" <json>
    *   <wholeJson> ::= <whole>
    *   <realJson> ::= <real>
    *   <stringJson> ::= <string>
    *   <booleanJson> ::= <boolean>
    *   <nullJson> ::= "null"
    *   <fieldJsonList> ::= <fieldJson> ("," <fieldJson>)*
    *   <jsonList> ::= <json> ("," <json>)*
    * 
    * Examples of 'json':
    *   "{}" => Json object
    *   "[1, 2, 3]" => Json array
    * 
    * Examples of 'objJson':
    *   "{}" => empty Json object
    *   "{\"key\": 42}" => Json object with key "key" and value 42
    * 
    * Examples of 'arrayJson':
    *   "[]" => empty Json array
    *   "[1, 2, 3]" => Json array with values 1, 2, and 3
    * 
    * Examples of 'fieldJson':
    *   "key: 42" => ("key", 42)
    *   "\"stringKey\": \"value\"" => ("stringKey", "value")
    * 
    * Examples of 'wholeJson':
    *   "42" => Json.fromInt(42)
    *   "-10" => -10
    * 
    * Examples of 'realJson':
    *   "3.14" => 3.14.asJson
    *   "-1.5E-2" => -0.015.asJson
    * 
    * Examples of 'stringJson':
    *   "\"hello\"" => Json.fromString("hello")
    *   "'world'" => Json.fromString("world")
    * 
    * Examples of 'booleanJson':
    *   "true" => true
    *   "false" => false
    * 
    * Example of 'nullJson':
    *   "null" => Json.Null
    */
  def json: Parser[Json] = objJson | arrayJson | realJson | wholeJson | stringJson | booleanJson | nullJson
  def objJson: Parser[Json] = "{" ~> repsep(fieldJson, ",") <~ "}" ^^ { Json.fromFields(_) }
  def arrayJson: Parser[Json] = "[" ~> repsep(json, ",") <~ "]" ^^ { Json.fromValues(_) }
  def fieldJson: Parser[(String, Json)] = (identifier | string) ~ ( ":" ~> json) ^^ {
    case id ~ va => (id, va)
  }
  def wholeJson: Parser[Json] = whole ^^ { Json.fromInt(_) }
  def realJson: Parser[Json] = real ^^ { _.asJson }
  def stringJson: Parser[Json] = string ^^ { Json.fromString(_) }
  def booleanJson: Parser[Json] = boolean ^^ { Json.fromBoolean(_)}
  def nullJson: Parser[Json] = "null" ^^^ Json.Null


  /**
    * A collection of path parsers
    * 
    * Example of 'root':
    *   "root" => Root
    * 
    * Examples of 'path':
    *   "root.key"
    *   ".key"
    *   ".key[condition]"
    * 
    * Examples of 'down':
    *   ".key"
    * 
    * Examples of 'filter':
    *   "[true]"
    *   "[.key != 'test']"
    */
  def root: Parser[PathExp] = "root" ^^^ Root
  def path: Parser[Path] = (root | down | filter) ~ rep(down | filter) ^^ {
    case head ~ tail => {
      Path(head :: tail)
    }
  }
  def down: Parser[PathExp] = "." ~> identifier ^^ { Attribute(_) }
  def filter: Parser[PathExp] = "[" ~> condition <~ "]" ^^ { Filter(_) }
  
  /**
    * A collection of condition parsers
    * 
    * Examples of 'brackets':
    *   "(value == 42)"
    *   "(key != 'test')"
    * 
    * Examples of 'condition':
    *   ".value == 42"
    *   ".key != 'test' && has(root.array)"
    * 
    * Examples of 'or':
    *   "true || false"
    *   "has(.array) || .array == 'hello'"
    * 
    * Examples of 'and':
    *   "true && false"
    *   "has(.array) && .array == 'hello'"
    * 
    * Examples of 'comparison':
    *   "value == 42"
    *   "has(.array)"
    *   "true"
    * 
    * Examples of 'equals':
    *   ".value == 42"
    *   ".key == 'test'"
    * 
    * Examples of 'notEquals':
    *   ".value != 42"
    *   ".key != 'test'"
    * 
    * Examples of 'not':
    *   "!.value
    *   "!has(.array)"
    *   "!(has(.value) || .value == "foo")"
    * 
    * Examples of 'has':
    *   "has(.array)"
    *   "has(root.key.subkey)"
    */
  def brackets: Parser[Condition] = "(" ~> condition <~ ")"
  def condition: Parser[Condition] = or
  def or: Parser[Condition] = rep1sep(and, "||") ^^ {
    case comps => comps.reduceLeft(Or)
  }
  def and: Parser[Condition] = rep1sep(comparison, "&&") ^^ {
    case comps => comps.reduceLeft(And)
  }
  def comparison: Parser[Condition] = brackets | equals | notEquals | not | has | trueCond | falseCond
  def trueCond: Parser[Condition] = "true" ^^^ True
  def falseCond: Parser[Condition] = "false" ^^^ False
  def equals: Parser[Equals] = value ~ ("==" ~> value) ^^ {
    case left ~ right => Equals(left, right)
  }
  def notEquals: Parser[NotEquals] = value ~ ("!=" ~> value) ^^ {
    case left ~ right => NotEquals(left, right)
  }
  def not: Parser[Not] = "!" ~> (brackets | not | has | trueCond | falseCond) ^^ { Not(_) }
  def has: Parser[Has] = "has(" ~> path <~ ")" ^^ { Has(_) }


  /**
    * A collection of condition value parsers
    * 
    * Examples of 'value':
    *   "root.key"
    *   "42"
    *   "'string'"
    */
  def value: Parser[Value] = path | (json ^^ { JsonValue(_) })


  /**
    * A collection of command parsers
    * 
    * Examples of 'command':
    *   "del(.key)"
    *   ".foo := 10"
    *   "<preparse> .array += 'item'"
    * 
    * Examples of 'delete':
    *   "del(.key)"
    *   "del(root.array)"
    * 
    * Examples of 'assign':
    *   ".key := 'value'"
    *   ".array[.type == 'docker'] := 42"
    * 
    * Examples of 'append':
    *   ".array += 'value'"
    * 
    * Examples of 'prepend':
    *   ".array +0= 'value'"
    * 
    * Examples of 'block':
    *   "del(.key); .foo := 'bar'"
    *   "<preparse> .platforms[.type == 'nextflow'].variant := 'vdsl3'"
    */
  def command: Parser[(Boolean, Command)] = opt("<preparse>") ~ (delete | assign | append | prepend) ^^ {
    case maybePreparse ~ cm => (maybePreparse.isDefined, cm)
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
  def block: Parser[ConfigMods] = rep1sep(command, "[;\n]+".r) <~ "[;\n]*".r ^^ { cmds =>
    val preparseCommands = cmds.filter(_._1).map(_._2)
    val postparseCommands = cmds.filter(!_._1).map(_._2)
    ConfigMods(
      postparseCommands = postparseCommands,
      preparseCommands = preparseCommands
    )
  }
}
