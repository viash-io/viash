:load src/main/scala/com/dataintuitive/viash/Parameters.scala
import implicits._

import io.circe.parser.decode
import io.circe.syntax._


val js = Map(
  "type" → "string",
  "name" → "help"
).asJson


val x = js.as[Parameter[_]]

x match { case Right(x) => x; case Left(x) => throw x }





val js = Map(
  "type" → "string",
  "name" → "help",
  "description" → "my help"
).asJson


val x = js.as[Parameter[_]]

x match { case Right(x) => x; case Left(x) => throw x }



val js = Map(
  "type" → "string",
  "name" → "help",
  "description" → "my help",
  "default" → "my default"
).asJson


val x = js.as[Parameter[_]]

val y = x match { case Right(x) => x; case Left(x) => throw x }



