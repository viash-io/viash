// :load src/main/scala/com/dataintuitive/viash/Parameters.scala

import com.dataintuitive.viash.functionality._

import scala.io.Source

import io.circe.yaml.parser
import io.circe.yaml.syntax._

import cats.syntax.either._
import io.circe._
import io.circe.generic.auto._
import io.circe.syntax._


//val pSpecStr = Source.fromFile("atoms/filter/portash.yaml").mkString
val pSpecStr = Source.fromFile("temp/mytest.yaml").mkString

val Right(json) = io.circe.yaml.parser.parse(pSpecStr)
    
val Right(fun) = json.as[Functionality]

val js = Map(
  "type" → "file",
  "name" → "help",
  "description" → "my help",
  "default" → "help.pdf",
  "mustExist" → "false"
).asJson
val x = js.as[FileParameter]

val js = Map(
  "type" → "file",
  "name" → "help",
  "description" → "my help",
  "default" → "help.pdf"
).asJson
val x = js.as[FileParameter]

/*
import io.circe.parser.decode
import io.circe.syntax._

val obj = List(
  StringParameter("name", description = Some("help")),
  DoubleParameter("name2", default = Some(0.2))
)

obj(0).asJson

val js = obj.asJson

val js = Map(
  "type" → "string",
  "name" → "help",
  "description" → "my help",
  "default" → "my default"
).asJson


val Right(x) = js.as[Parameter[_]]

x.asJson

x match {
  case sp: StringParameter => sp.asJson
}

y.asInstanceOf[StringParameter].asJson
*/
