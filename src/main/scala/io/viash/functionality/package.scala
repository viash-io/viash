// /*
//  * Copyright (C) 2020  Data Intuitive
//  *
//  * This program is free software: you can redistribute it and/or modify
//  * it under the terms of the GNU General Public License as published by
//  * the Free Software Foundation, either version 3 of the License, or
//  * (at your option) any later version.
//  *
//  * This program is distributed in the hope that it will be useful,
//  * but WITHOUT ANY WARRANTY; without even the implied warranty of
//  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//  * GNU General Public License for more details.
//  *
//  * You should have received a copy of the GNU General Public License
//  * along with this program.  If not, see <http://www.gnu.org/licenses/>.
//  */

// package io.viash

// import io.circe.generic.extras.semiauto.{deriveConfiguredDecoder, deriveConfiguredEncoder}
// import io.circe.{Decoder, Encoder, Json}
// import io.circe.ACursor

// import io.viash.helpers.Logging
// import io.circe.JsonObject


// package object functionality extends Logging {
//   // import implicits

//   import functionality.arguments._
//   import functionality.resources._
//   import config.Status._
//   import functionality.dependencies._
//   import io.viash.helpers.circe._
//   import io.viash.helpers.circe.DeriveConfiguredDecoderFullChecks._
//   import io.viash.helpers.circe.DeriveConfiguredDecoderWithDeprecationCheck._
//   import io.viash.helpers.circe.DeriveConfiguredDecoderWithValidationCheck._
//   import io.viash.helpers.circe.DeriveConfiguredEncoderStrict._

//   // encoder and decoder for Functionality
//   implicit val encodeFunctionality: Encoder.AsObject[Config] = deriveConfiguredEncoderStrict[Config]

//   // merge arguments and argument_groups into argument_groups
//   implicit val decodeFunctionality: Decoder[Config] = deriveConfiguredDecoderFullChecks[Config]
//     .prepare {
//       _.withFocus{ json =>
//         json.asObject match {
//           case None => json
//           case Some(jo) => 
//             val arguments = jo.apply("arguments")
//             val argument_groups = jo.apply("argument_groups")

//             val newJsonObject = (arguments, argument_groups) match {
//               case (None, _) => jo
//               case (Some(args), None) => 
//                 jo.add("argument_groups", Json.fromValues(List(
//                   Json.fromJsonObject(
//                     JsonObject(
//                       "name" -> Json.fromString("Arguments"),
//                       "arguments" -> args
//                     )
//                   )
//                 )))
//               case (Some(args), Some(arg_groups)) =>
//                 // determine if we should prepend or append arguments to argument_groups
//                 val prepend = jo.keys.find(s => s == "argument_groups" || s == "arguments") == Some("arguments")
//                 def combinerSeq(a: Vector[Json], b: Seq[Json]) = if (prepend) b ++: a else a :++ b
//                 def combiner(a: Vector[Json], b: Json) = if (prepend) b +: a else a :+ b

//                 // get the argument group named 'Arguments' from arg_groups
//                 val argumentsGroup = arg_groups.asArray.flatMap(_.find(_.asObject.exists(_.apply("name").exists(_ == Json.fromString("Arguments")))))
//                 argumentsGroup match {
//                   case None =>
//                     // no argument_group with name 'Argument' exists, so just add arguments as a new argument group
//                     jo.add("argument_groups", Json.fromValues(
//                       combiner(
//                         arg_groups.asArray.get,
//                         Json.fromJsonObject(
//                           JsonObject(
//                             "name" -> Json.fromString("Arguments"),
//                             "arguments" -> args
//                           )
//                         )
//                       )
//                     ))
//                   case Some(ag) =>
//                     // argument_group with name 'Argument' exists, so add arguments to this argument group
//                     val newAg = ag.asObject.get.add("arguments",
//                       Json.fromValues(
//                         combinerSeq(
//                           ag.asObject.get.apply("arguments").get.asArray.get,
//                           args.asArray.get
//                         )
//                       )
//                     )
//                     jo.add("argument_groups", Json.fromValues(arg_groups.asArray.get.map{
//                       case ag if ag == argumentsGroup.get => Json.fromJsonObject(newAg)
//                       case ag => ag
//                     }))
//                 }
//             }
//             Json.fromJsonObject(newJsonObject.remove("arguments"))
//         }
      
//     }}


// }
