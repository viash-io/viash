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

package io.viash.engines

import io.circe.{Decoder, Encoder, Json}

package object docker {
  import io.viash.helpers.circe._

  // encoder and decoder for resolvevolume
  implicit val encodeResolveVolume: Encoder[DockerResolveVolume] = Encoder.instance {
    v => Json.fromString(v.toString)
  }
  implicit val decodeResolveVolume: Decoder[DockerResolveVolume] = Decoder.instance {
    cursor =>
      cursor.value.as[String].map(s =>
        s.toLowerCase() match {
          case "manual" => Manual
          case "auto" | "automatic" => Automatic
        }
      )
  }

}

