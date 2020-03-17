import com.dataintuitive.viash.functionality._
import com.dataintuitive.viash.platform._

import io.circe.yaml.parser
import io.circe.yaml.syntax._

import cats.syntax.either._
import io.circe._
import io.circe.generic.auto._
import io.circe.syntax._

import io.circe.parser.decode

val obj = List(
  StringParameter("name", description = Some("help")),
  DoubleParameter("name2", default = Some(0.2))
)

obj.asJson



val x = decode[List[Parameter[_]]]("""
[
  {
    "type": "file",
    "name": "help", 
    "description": "my descr", 
    "default": "help.pdf",
    "mustExist": true
  },
  {
    "type": "string",
    "name": "test"
  },
  {
    "type": "double",
    "name": "test2",
    "default": 0.1
  },
  {
    "type": "integer",
    "name": "test3",
    "default": 1
  },
  {
    "type": "boolean",
    "name": "test4",
    "default": false,
    "description": "help"
  }
]
""")


val x = decode[REnvironment]("""
{
  "packages": ["help"],
  "github": ["nope"]
}
""")
