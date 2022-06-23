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

package com.dataintuitive.viash.functionality

import io.circe.{Decoder, Encoder, Json}
import io.circe.generic.extras.semiauto.{deriveConfiguredDecoder, deriveConfiguredEncoder}
import cats.syntax.functor._

import java.net.URI // for .widen
import io.circe.ACursor

package object resources {

  import com.dataintuitive.viash.helpers.Circe._

  implicit val encodeURI: Encoder[URI] = Encoder.instance {
    uri => Json.fromString(uri.toString)
  }
  implicit val decodeURI: Decoder[URI] = Decoder.instance {
    cursor => cursor.value.as[String].map(new URI(_))
  }

  // encoders and decoders for Object
  implicit val encodeBashScript: Encoder.AsObject[BashScript] = deriveConfiguredEncoder
  implicit val encodePythonScript: Encoder.AsObject[PythonScript] = deriveConfiguredEncoder
  implicit val encodeRScript: Encoder.AsObject[RScript] = deriveConfiguredEncoder
  implicit val encodeJavaScriptScript: Encoder.AsObject[JavaScriptScript] = deriveConfiguredEncoder
  implicit val encodeScalaScript: Encoder.AsObject[ScalaScript] = deriveConfiguredEncoder
  implicit val encodeCSharpScript: Encoder.AsObject[CSharpScript] = deriveConfiguredEncoder
  implicit val encodeExecutable: Encoder.AsObject[Executable] = deriveConfiguredEncoder
  implicit val encodePlainFile: Encoder.AsObject[PlainFile] = deriveConfiguredEncoder

  implicit def encodeResource[A <: Resource]: Encoder[A] = Encoder.instance {
    par =>
      val typeJson = Json.obj("type" → Json.fromString(par.companion.`type`))
      val objJson = par match {
        case s: BashScript => encodeBashScript(s)
        case s: PythonScript => encodePythonScript(s)
        case s: RScript => encodeRScript(s)
        case s: JavaScriptScript => encodeJavaScriptScript(s)
        case s: ScalaScript => encodeScalaScript(s)
        case s: CSharpScript => encodeCSharpScript(s)
        case s: Executable => encodeExecutable(s)
        case s: PlainFile => encodePlainFile(s)
      }
      objJson deepMerge typeJson
  }

  def setDestDefault(default: String) = (aCursor: ACursor) => {aCursor.withFocus(
    _.mapObject{ obj =>	
      // when json defines 'text' but no 'dest' set
      // if has 'path' -> switch 'path' to 'dest'
      // else if no 'path' or 'dest' -> set 'dest' to default value
      if (!obj.contains("text") || obj.contains("dest")) {
        obj
      } else if (obj.contains("path")) {
        obj.add("dest", obj("path").get).remove("path")
      } else {
        obj.add("dest", Json.fromString(default))
      }
    //   if (obj.contains("text") && !obj.contains("dest")) {
    //     if (obj.contains("path"))
    //       obj.add("dest", obj("path").get).remove("path")
    //     else 
    //       obj.add("dest", Json.fromString(default))
    //   } else {
    //     obj
    //   }
    }
  )}

  def prepareScript(companion: ResourceCompanion, prefix: String = "script") = {
    val f1 = setDestDefault(s"./$prefix.${companion.extension}")
    val f2 = removeType
    f1 andThen f2
  }
  val removeType = (aCursor: ACursor) => {
    val c2 = aCursor.downField("type").delete
    if (c2.succeeded) c2 else aCursor
  }

  implicit val decodeBashScript: Decoder[BashScript] = deriveConfiguredDecoder[BashScript]
    .prepare(prepareScript(BashScript))
  implicit val decodePythonScript: Decoder[PythonScript] = deriveConfiguredDecoder[PythonScript]
    .prepare(prepareScript(PythonScript))
  implicit val decodeRScript: Decoder[RScript] = deriveConfiguredDecoder[RScript]
    .prepare(prepareScript(RScript))
  implicit val decodeJavaScriptScript: Decoder[JavaScriptScript] = deriveConfiguredDecoder[JavaScriptScript]
    .prepare(prepareScript(JavaScriptScript))
  implicit val decodeScalaScript: Decoder[ScalaScript] = deriveConfiguredDecoder[ScalaScript]
    .prepare(prepareScript(ScalaScript))
  implicit val decodeCSharpScript: Decoder[CSharpScript] = deriveConfiguredDecoder[CSharpScript]
    .prepare(prepareScript(CSharpScript))
  implicit val decodeExecutable: Decoder[Executable] = deriveConfiguredDecoder[Executable]
    .prepare(removeType)
  implicit val decodePlainFile: Decoder[PlainFile] = deriveConfiguredDecoder[PlainFile]
    .prepare(prepareScript(PlainFile, prefix = "file"))

  implicit def decodeResource: Decoder[Resource] = Decoder.instance {
    cursor =>
      val decoder: Decoder[Resource] =
        cursor.downField("type").as[String] match {
          case Right("bash_script") => decodeBashScript.widen
          case Right("python_script") => decodePythonScript.widen
          case Right("r_script") => decodeRScript.widen
          case Right("javascript_script") => decodeJavaScriptScript.widen
          case Right("scala_script") => decodeScalaScript.widen
          case Right("csharp_script") => decodeCSharpScript.widen
          case Right("executable") => decodeExecutable.widen
          case Right("file") => decodePlainFile.widen
          case Right(typ) => throw new RuntimeException(
            "File type " + typ + " is not recognised. Should be one of " +
              "'bash_script', 'python_script', 'r_script', 'javascript_script', 'scala_script', 'csharp_script', 'executable', or 'file'."
          )
          case Left(_) => decodePlainFile.widen // default is a simple file
        }
      decoder(cursor)
  }
}