package com.dataintuitive.viash

import java.io.File
import io.circe.{ Decoder, Encoder, HCursor, Json }

trait Parameter[Type] {
  val `type`: String
  val name: String
  val description: Option[String]
  val default: Option[Type]
  
  def validate(value: Type): Boolean = {
    true
  }
}

case class StringParameter(
    name: String,
    description: Option[String] = None,
    default: Option[String] = None
) extends Parameter[String] {
  override val `type` = "string"
}

case class IntegerParameter(
    name: String,
    description: Option[String] = None,
    default: Option[Int] = None
) extends Parameter[Int] {
  override val `type` = "integer"
}

case class DoubleParameter(
    name: String,
    description: Option[String] = None,
    default: Option[Double] = None
) extends Parameter[Double] {
  override val `type` = "double"
}

case class FileParameter(
    name: String,
    description: Option[String] = None,
    default: Option[File] = None,
    must_exist: Boolean = false
) extends Parameter[File] {
  override val `type` = "file"
  
  override def validate(value: File) = {
    !must_exist || value.exists
  }
}

object implicits {
  implicit val encodeStringParameter: Encoder[StringParameter] = new Encoder[StringParameter] {
    final def apply(par: StringParameter): Json = {
      val li = List(
        List(
          "type" → Json.fromString(par.`type`),
          "name" → Json.fromString(par.name)
        ),
        par.description.toList.map("description" → Json.fromString(_)),
        par.default.toList.map("default" → Json.fromString(_))
      ).flatten
      
      Json.fromFields(li)
    }
  }
  
  implicit val encodeParameter: Encoder[Parameter[_]] = new Encoder[Parameter[_]] {
    final def apply(par: Parameter[_]): Json = Json.arr(
      par match {
        case s: StringParameter => encodeStringParameter(s)
      }
    )
  }
  
  implicit val decodeParameter: Decoder[Parameter[_]] = new Decoder[Parameter[_]] {
    final def apply(c: HCursor): Decoder.Result[Parameter[_]] = {
      val z = c.downField("type").as[String].toOption
      z match {
        case Some("string") => 
          for {
            name <- c.downField("name").as[String]
            description <- c.downField("description").as[Option[String]]
            default <- c.downField("default").as[Option[String]]
          } yield {
            StringParameter(name = name, description = description, default = default)
          }
        case _ =>
          throw new RuntimeException("Type " + z + " is not recognised.")
      }
    }
  }
  
  
  
  //  def fromMap(map: Map[String, String]): Parameter[_] = {
  //    return map("type") match {
  //      case "string" => StringParameter(
  //        map("name"),
  //        map.get("description"), 
  //        map.get("default")
  //      )
  //      case "integer" => IntegerParameter(
  //        map("name"),
  //        map.get("description"), 
  //        map.get("default").map(_.toInt)
  //      )
  //      case "double" => DoubleParameter(
  //        map("name"),
  //        map.get("description"), 
  //        map.get("default").map(_.toDouble)
  //      )
  //      case "file" => FileParameter(
  //        map("name"),
  //        map.get("description"), 
  //        map.get("default").map(new File(_))
  //      )
  //    }
  //  }

          
//          
//        StringParameter(
//          c.downField("name").as[String],
//          map.get("description"), 
//          map.get("default")
//        )
//        case "integer" => IntegerParameter(
//          map("name"),
//          map.get("description"), 
//          map.get("default").map(_.toInt)
//        )
//        case "double" => DoubleParameter(
//          map("name"),
//          map.get("description"), 
//          map.get("default").map(_.toDouble)
//        )
//        case "file" => FileParameter(
//          map("name"),
//          map.get("description"), 
//          map.get("default").map(new File(_))
//        )
}