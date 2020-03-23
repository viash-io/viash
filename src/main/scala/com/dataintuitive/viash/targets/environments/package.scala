package com.dataintuitive.viash.targets

import io.circe.{ Decoder, Encoder, Json }
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import cats.syntax.functor._ // for .widen
import com.dataintuitive.viash.targets.environments.PythonEnvironment
import com.dataintuitive.viash.targets.environments.REnvironment

package object environments {  
  implicit val encodeVolume: Encoder[Volume] = deriveEncoder
  implicit val decodeVolume: Decoder[Volume] = deriveDecoder
  
  implicit val encodeREnvironment: Encoder[REnvironment] = deriveEncoder
  implicit val decodeREnvironment: Decoder[REnvironment] = deriveDecoder
  
  implicit val encodePythonEnvironment: Encoder[PythonEnvironment] = deriveEncoder
  implicit val decodePythonEnvironment: Decoder[PythonEnvironment] = deriveDecoder
  
  implicit val encodeAptEnvironment: Encoder[AptEnvironment] = deriveEncoder
  implicit val decodeAptEnvironment: Decoder[AptEnvironment] = deriveDecoder
}