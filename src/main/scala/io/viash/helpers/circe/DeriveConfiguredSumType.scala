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

package io.viash.helpers.circe

import io.circe.{Decoder, Encoder, Json, JsonObject}
import scala.reflect.ClassTag

/**
 * Builds an Encoder/Decoder for a hierarchy that is discriminated by a field (e.g. `type`),
 * where each subtype already carries its own hand-written (or FullChecks/Strict-derived)
 * codec. This exists because these hierarchies can't be made `sealed` without merging every
 * subtype into a single file (Scala requires sealed subtypes to live alongside the parent),
 * so circe's own Mirror-based coproduct derivation isn't available to them.
 */
object DeriveConfiguredSumType {

  /** A single subtype of `Base`, tagged with the discriminator value it's written/read as. */
  final case class Branch[Base](tag: String, cls: Class[_], decode: Decoder[Base], encode: Base => JsonObject)

  def branch[Base, A <: Base](tag: String)(implicit ct: ClassTag[A], dec: Decoder[A], enc: Encoder.AsObject[A]): Branch[Base] =
    Branch(tag, ct.runtimeClass, dec.map(a => a: Base), (b: Base) => enc.encodeObject(b.asInstanceOf[A]))

  def encoder[Base](branches: List[Branch[Base]]): Encoder.AsObject[Base] = Encoder.AsObject.instance { value =>
    val b = branches.find(_.cls.isInstance(value))
      .getOrElse(throw new IllegalStateException(s"No encoder registered for ${value.getClass.getName}"))
    // deepMerge (not JsonObject.add): the discriminator field is already part of the model's own
    // fields (e.g. `type: String = "bash_script"`), so this is re-merging an existing key. deepMerge
    // moves re-merged keys to the front, which several golden-output tests rely on.
    (Json.fromJsonObject(b.encode(value)) deepMerge Json.obj("type" -> Json.fromString(b.tag))).asObject.get
  }

  /**
   * @param fieldName the name of the discriminator field, e.g. "type"
   * @param branches the subtypes to dispatch to, keyed by their discriminator value
   * @param whenInvalid decoder to use when the discriminator field's value doesn't match any branch;
   *                     receives the offending value and the list of valid tags
   * @param whenMissing decoder to fall back to when the discriminator field is absent or isn't a string;
   *                     defaults to rethrowing the original decoding failure, matching the historical
   *                     behaviour of these hand-written dispatchers
   */
  def decoder[Base](
    fieldName: String,
    branches: List[Branch[Base]],
    whenInvalid: (String, List[String]) => Decoder[Base],
    whenMissing: Option[Decoder[Base]] = None
  ): Decoder[Base] = Decoder.instance { cursor =>
    cursor.downField(fieldName).as[String] match {
      case Right(tag) =>
        branches.find(_.tag == tag) match {
          case Some(b) => b.decode(cursor)
          case None => whenInvalid(tag, branches.map(_.tag))(cursor)
        }
      case Left(failure) =>
        whenMissing match {
          case Some(dec) => dec(cursor)
          case None => throw failure
        }
    }
  }
}
