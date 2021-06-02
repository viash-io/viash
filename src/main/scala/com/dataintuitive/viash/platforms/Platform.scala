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

package com.dataintuitive.viash.platforms

import com.dataintuitive.viash.functionality.Functionality
import com.dataintuitive.viash.helpers.IO
import io.circe.yaml.parser
import java.net.URI
import requirements._
import com.dataintuitive.viash.config.Version

trait Platform {
  val oType: String
  val id: String

  val hasSetup: Boolean

  def modifyFunctionality(functionality: Functionality): Functionality

  val requirements: List[Requirements]
}

object Platform {
  def parse(uri: URI): Platform = {
    val str = IO.read(uri)
    parser.parse(str)
      .fold(throw _, _.as[Platform])
      .fold(throw _, identity)
  }

  def read(path: String): Platform = {
    parse(IO.uri(path))
  }
}
