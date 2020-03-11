// :load src/main/scala/com/dataintuitive/viash/Parameters.scala

import com.dataintuitive.viash._
import implicits._

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
