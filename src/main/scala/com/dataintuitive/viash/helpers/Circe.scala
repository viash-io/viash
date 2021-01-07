package com.dataintuitive.viash.helpers

import io.circe.{Decoder, Encoder, Json}
import io.circe.generic.extras.Configuration

object Circe {
  implicit val customConfig: Configuration = Configuration.default.withDefaults

  // encoder and decoder for Either
  implicit def encodeEither[A,B](implicit ea: Encoder[A], eb: Encoder[B]): Encoder[Either[A,B]] = new Encoder[Either[A, B]] {
    final def apply(eit: Either[A,B]): Json =
      eit match {
        case Left(a) => ea(a)
        case Right(b) => eb(b)
      }
  }

  implicit def decodeEither[A,B](implicit a: Decoder[A], b: Decoder[B]): Decoder[Either[A,B]] = {
    val l: Decoder[Either[A,B]] = a.map(Left.apply)
    val r: Decoder[Either[A,B]] = b.map(Right.apply)
    l or r
  }
}