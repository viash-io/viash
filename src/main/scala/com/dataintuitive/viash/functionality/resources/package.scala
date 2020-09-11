package com.dataintuitive.viash.functionality

import io.circe.{Decoder, Encoder, Json}
import io.circe.generic.extras.semiauto.{deriveConfiguredDecoder, deriveConfiguredEncoder}
import cats.syntax.functor._ // for .widen

package object resources {

  import com.dataintuitive.viash.helpers.Circe._

  // encoders and decoders for Object
  implicit val encodeBashScript: Encoder.AsObject[BashScript] = deriveConfiguredEncoder
  implicit val encodePythonScript: Encoder.AsObject[PythonScript] = deriveConfiguredEncoder
  implicit val encodeRScript: Encoder.AsObject[RScript] = deriveConfiguredEncoder
  implicit val encodeExecutable: Encoder.AsObject[Executable] = deriveConfiguredEncoder
  implicit val encodePlainFile: Encoder.AsObject[PlainFile] = deriveConfiguredEncoder

  implicit def encodeResource[A <: Resource]: Encoder[A] = Encoder.instance {
    par =>
      val typeJson = Json.obj("type" → Json.fromString(par.`type`))
      val objJson = par match {
        case s: BashScript => encodeBashScript(s)
        case s: PythonScript => encodePythonScript(s)
        case s: RScript => encodeRScript(s)
        case s: Executable => encodeExecutable(s)
        case s: PlainFile => encodePlainFile(s)
      }
      objJson deepMerge typeJson
  }

  implicit val decodeBashScript: Decoder[BashScript] = deriveConfiguredDecoder
  implicit val decodePythonScript: Decoder[PythonScript] = deriveConfiguredDecoder
  implicit val decodeRScript: Decoder[RScript] = deriveConfiguredDecoder
  implicit val decodeExecutable: Decoder[Executable] = deriveConfiguredDecoder
  implicit val decodePlainFile: Decoder[PlainFile] = deriveConfiguredDecoder

  implicit def decodeResource: Decoder[Resource] = Decoder.instance {
    cursor =>
      val decoder: Decoder[Resource] =
        cursor.downField("type").as[String] match {
          case Right("bash_script") => decodeBashScript.widen
          case Right("python_script") => decodePythonScript.widen
          case Right("r_script") => decodeRScript.widen
          case Right("executable") => decodeExecutable.widen
          case Right("file") => decodePlainFile.widen
          case Right(typ) => throw new RuntimeException(
            "File type " + typ + " is not recognised. Should be one of " +
              "'bash_script', 'python_script', 'r_script', 'executable', or 'file'."
          )
          case Left(_) => decodePlainFile.widen // default is a simple file
        }

      decoder(cursor)
  }
}