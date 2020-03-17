package com.dataintuitive.viash

import java.io.File
import io.circe.{ Decoder, Encoder }
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

package object platform {
  implicit val encodeVolume: Encoder[Volume] = deriveEncoder
  implicit val decodeVolume: Decoder[Volume] = deriveDecoder
  
  implicit val encodeREnvironment: Encoder[REnvironment] = deriveEncoder
  implicit val decodeREnvironment: Decoder[REnvironment] = deriveDecoder
  
  implicit val encodePythonEnvironment: Encoder[PythonEnvironment] = deriveEncoder
  implicit val decodePythonEnvironment: Decoder[PythonEnvironment] = deriveDecoder
  
  implicit val encodeDockerPlatform: Encoder[DockerPlatform] = deriveEncoder
  implicit val decodeDockerPlatform: Decoder[DockerPlatform] = deriveDecoder
  
  implicit val encodeNativePlatform: Encoder[NativePlatform] = deriveEncoder
  implicit val decodeNativePlatform: Decoder[NativePlatform] = deriveDecoder
  
  implicit val encodePlatform: Encoder[Platform] = deriveEncoder
  implicit val decodePlatform: Decoder[Platform] = deriveDecoder
}