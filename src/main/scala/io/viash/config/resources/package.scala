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
import io.circe.generic.extras.semiauto.{deriveConfiguredDecoder, deriveConfiguredEncoder}
import cats.syntax.functor._

import java.net.URI // for .widen
import io.circe.ACursor

package object resources {

  import io.viash.helpers.circe.DeriveConfiguredDecoderFullChecks._
  import io.viash.helpers.circe._
  import io.viash.helpers.circe.DeriveConfiguredEncoderStrict._

  implicit val encodeURI: Encoder[URI] = Encoder.instance {
    uri => Json.fromString(uri.toString)
  }
  implicit val decodeURI: Decoder[URI] = Decoder.instance {
    cursor => cursor.value.as[String].map(new URI(_))
  }

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

  implicit def encodeResource[A <: Resource]: Encoder[A] = Encoder.instance {
    par =>
      val typeJson = Json.obj("type" -> Json.fromString(par.`type`))
      val objJson = par match {
        case s: BashScript => encodeBashScript(s)
        case s: PythonScript => encodePythonScript(s)
        case s: RScript => encodeRScript(s)
        case s: JavaScriptScript => encodeJavaScriptScript(s)
        case s: NextflowScript => encodeNextflowScript(s)
        case s: ScalaScript => encodeScalaScript(s)
        case s: CSharpScript => encodeCSharpScript(s)
        case s: Executable => encodeExecutable(s)
        case s: PlainFile => encodePlainFile(s)
      }
      objJson deepMerge typeJson
  }

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

  implicit def decodeResource: Decoder[Resource] = Decoder.instance {
    cursor =>
      val decoder: Decoder[Resource] =
        cursor.downField("type").as[String] match {
          case Right("bash_script") => decodeBashScript.widen
          case Right("python_script") => decodePythonScript.widen
          case Right("r_script") => decodeRScript.widen
          case Right("javascript_script") => decodeJavaScriptScript.widen
          case Right("nextflow_script") => decodeNextflowScript.widen
          case Right("scala_script") => decodeScalaScript.widen
          case Right("csharp_script") => decodeCSharpScript.widen
          case Right("executable") => decodeExecutable.widen
          case Right("file") => decodePlainFile.widen
          case Right(typ) => 
            DeriveConfiguredDecoderWithValidationCheck.invalidSubTypeDecoder[BashScript](typ, Script.companions.map(c => c.`type`) ++ List("executable", "file")).widen
          case Left(_) => decodePlainFile.widen // default is a simple file
        }

      decoder(cursor)
  }
}