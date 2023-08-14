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

package io.viash.functionality

import io.viash.schemas._

@description("Computational requirements related to running the component.")
@since("Viash 0.6.0")
case class ComputationalRequirements(
  @description("The maximum number of (logical) cpus a component is allowed to use.")
  @example("cpus: 10", "yaml")
  cpus: Option[Int] = None,
  @description("The maximum amount of memory a component is allowed to allocate. Unit must be one of B, KB, MB, GB, TB or PB.")
  @example("memory: 10GB", "yaml")
  memory: Option[String] = None,
  @description("A list of commands which should be present on the system for the script to function.")
  @example("commands: [ which, bash, awk, date, grep, egrep, ps, sed, tail, tee ]", "yaml")
  @default("Empty")
  commands: List[String] = Nil
) {

  def memoryAsBytes: Option[BigInt] = {
    val Regex = "^([0-9]+) *([kmgtp]b?|b)$".r
    val lookup = Map(
      "b" -> 0,
      "kb" -> 1,
      "mb" -> 2,
      "gb" -> 3,
      "tb" -> 4,
      "pb" -> 5
    )
    memory.map(_.toLowerCase()) match {
      case Some(Regex(amnt, unit)) => 
        val amntBigInt = BigInt(amnt)
        val multiplier = BigInt(1024)
        val exp = lookup(unit)
        Some(amntBigInt * multiplier.pow(exp))
      case Some(m) =>
        throw new RuntimeException(s"Invalid value \"$m\" as memory computational requirement.")
      case None => None
    }
  }
}