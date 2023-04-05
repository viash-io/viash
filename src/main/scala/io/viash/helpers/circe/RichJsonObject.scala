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

import io.circe.Json
import io.circe.JsonObject

class RichJsonObject(jsonObject: JsonObject) {
  /**
    * If the Json is a JsonObject, apply f on all key-value pairs.
    *
    * @param fun A method to apply over key-value pairs in a JsonObject
    * @return A modified Json
    */
  def map(f: ((String, Json)) => (String, Json)): JsonObject = {
    JsonObject(jsonObject.toList.map(f): _*)
  }

}