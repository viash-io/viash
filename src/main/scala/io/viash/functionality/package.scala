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

package io.viash

// import io.circe.generic.extras.semiauto.{deriveConfiguredDecoder, deriveConfiguredEncoder}
import io.circe.{Decoder, Encoder, Json}
import io.circe.ACursor

import io.viash.helpers.Logging
import io.circe.JsonObject
import io.viash.config.arguments.decodeDataArgument

import config.ArgumentGroup
import config.Author
import config.ComputationalRequirements
import config.Links
import config.References
import config.Status._
import config.arguments._

package object functionality extends Logging {
  // import implicits
  import io.viash.helpers.circe._

  import io.viash.config.{decodeAuthor, decodeArgumentGroup, decodeStatus, decodeComputationalRequirements, decodeReferences, decodeLinks}
  import io.viash.config.resources.decodeResource
  import io.viash.config.dependencies.{decodeDependency, decodeRepositoryWithName}

  // encoder and decoder for Functionality
  // implicit val encodeFunctionality: Encoder.AsObject[Functionality] = deriveConfiguredEncoderStrict[Functionality]
  implicit val decodeFunctionality: Decoder[Functionality] = deriveConfiguredDecoderFullChecks

}
