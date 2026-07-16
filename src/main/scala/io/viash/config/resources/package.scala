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

package io.viash.config

import io.circe.{Decoder, Encoder, Json}
import cats.syntax.functor._ // for .widen

import io.circe.ACursor
import io.viash.helpers.circe.DeriveConfiguredSumType
import io.viash.helpers.circe.DeriveConfiguredSumType.branch

package object resources {

  import io.viash.helpers.circe._

  // implicit val encodeURI: Encoder[URI] = Encoder.instance {
  //   uri => Json.fromString(uri.toString)
  // }
  // implicit val decodeURI: Decoder[URI] = Decoder.instance {
  //   cursor => cursor.value.as[String].map(new URI(_))
  // }

  // encoders and decoders for Object
  implicit val encodeBashScript: Encoder.AsObject[BashScript] = deriveConfiguredEncoderStrict[BashScript]
  implicit val encodePythonScript: Encoder.AsObject[PythonScript] = deriveConfiguredEncoderStrict[PythonScript]
  implicit val encodeRScript: Encoder.AsObject[RScript] = deriveConfiguredEncoderStrict[RScript]
  implicit val encodeJavaScriptScript: Encoder.AsObject[JavaScriptScript] = deriveConfiguredEncoderStrict[JavaScriptScript]
  implicit val encodeNextflowScript: Encoder.AsObject[NextflowScript] = deriveConfiguredEncoderStrict[NextflowScript]
  implicit val encodeScalaScript: Encoder.AsObject[ScalaScript] = deriveConfiguredEncoderStrict[ScalaScript]
  implicit val encodeCSharpScript: Encoder.AsObject[CSharpScript] = deriveConfiguredEncoderStrict[CSharpScript]
  implicit val encodeExecutable: Encoder.AsObject[Executable] = deriveConfiguredEncoderStrict[Executable]
  implicit val encodePlainFile: Encoder.AsObject[PlainFile] = deriveConfiguredEncoderStrict[PlainFile]

  val setDestToPathOrDefault = (default: String) => (aCursor: ACursor) => {aCursor.withFocus(js => {
    js.mapObject{ obj =>
      // when json defines 'text' but no 'dest' set
      // if has 'path' -> switch 'path' to 'dest'
      // else if no 'path' or 'dest' -> set 'dest' to default value
      if (obj.contains("text") && !obj.contains("dest")) {
        if (obj.contains("path"))
          obj.add("dest", obj("path").get).remove("path")
        else
          obj.add("dest", Json.fromString(default))
      } else {
        obj
      }
    }
  })}

  implicit val decodeBashScript: Decoder[BashScript] = deriveConfiguredDecoderFullChecks[BashScript].prepare { setDestToPathOrDefault("./script.sh") }
  implicit val decodePythonScript: Decoder[PythonScript] = deriveConfiguredDecoderFullChecks[PythonScript].prepare { setDestToPathOrDefault("./script.py") }
  implicit val decodeRScript: Decoder[RScript] = deriveConfiguredDecoderFullChecks[RScript].prepare { setDestToPathOrDefault("./script.R") }
  implicit val decodeJavaScriptScript: Decoder[JavaScriptScript] = deriveConfiguredDecoderFullChecks[JavaScriptScript].prepare { setDestToPathOrDefault("./script.js") }
  implicit val decodeNextflowScript: Decoder[NextflowScript] = deriveConfiguredDecoderFullChecks[NextflowScript].prepare { setDestToPathOrDefault("./script.nf") }
  implicit val decodeScalaScript: Decoder[ScalaScript] = deriveConfiguredDecoderFullChecks[ScalaScript].prepare { setDestToPathOrDefault("./script.scala") }
  implicit val decodeCSharpScript: Decoder[CSharpScript] = deriveConfiguredDecoderFullChecks[CSharpScript].prepare { setDestToPathOrDefault("./script.csx") }
  implicit val decodeExecutable: Decoder[Executable] = deriveConfiguredDecoderFullChecks
  implicit val decodePlainFile: Decoder[PlainFile] = deriveConfiguredDecoderFullChecks[PlainFile].prepare { setDestToPathOrDefault("./text.txt") }

  // must come after the individual encode*/decode* vals above: each branch() call resolves them
  // implicitly, and package object vals initialize in textual order, so referencing them earlier
  // would see them as still-uninitialized (null)
  private val resourceBranches: List[DeriveConfiguredSumType.Branch[Resource]] = List(
    branch[Resource, BashScript]("bash_script"),
    branch[Resource, PythonScript]("python_script"),
    branch[Resource, RScript]("r_script"),
    branch[Resource, JavaScriptScript]("javascript_script"),
    branch[Resource, NextflowScript]("nextflow_script"),
    branch[Resource, ScalaScript]("scala_script"),
    branch[Resource, CSharpScript]("csharp_script"),
    branch[Resource, Executable]("executable"),
    branch[Resource, PlainFile]("file"),
  )

  implicit val encodeResource: Encoder[Resource] = DeriveConfiguredSumType.encoder(resourceBranches)

  implicit val decodeResource: Decoder[Resource] = DeriveConfiguredSumType.decoder[Resource](
    "type",
    resourceBranches,
    whenInvalid = (typ, validTypes) => DeriveConfiguredDecoderWithValidationCheck.invalidSubTypeDecoder[BashScript](typ, validTypes).widen,
    whenMissing = Some(decodePlainFile.widen) // default is a simple file
  )
}