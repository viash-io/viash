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

package io.viash.config

import io.viash.schemas._

@description("Computational requirements related to running the component.")
@since("Viash 0.6.0")
case class ComputationalRequirements(
  @description("The maximum number of (logical) cpus a component is allowed to use.")
  @example("cpus: 10", "yaml")
  cpus: Option[Int] = None,
  @description("The maximum amount of memory a component is allowed to allocate. Unit must be one of B, KB, MB, GB, TB or PB for SI units (1000-base), or KiB, MiB, GiB, TiB or PiB for binary IEC units (1024-base).")
  @example("memory: 10GB", "yaml")
  memory: Option[String] = None,
  @description("A list of commands which should be present on the system for the script to function.")
  @example("commands: [ which, bash, awk, date, grep, egrep, ps, sed, tail, tee ]", "yaml")
  @default("Empty")
  commands: List[String] = Nil
) {

  def memoryAsBytes: Option[BigInt] = {
    val Regex = "^([0-9]+) *([kmgtp]i?b?|b)$".r
    val lookup = Map(
      "b" -> (0, 1000),
      "kb" -> (1, 1000),
      "mb" -> (2, 1000),
      "gb" -> (3, 1000),
      "tb" -> (4, 1000),
      "pb" -> (5, 1000),
      "kib" -> (1, 1024),
      "mib" -> (2, 1024),
      "gib" -> (3, 1024),
      "tib" -> (4, 1024),
      "pib" -> (5, 1024)
    )
    memory.map(_.toLowerCase()) match {
      case Some(Regex(amnt, unit)) => 
        val (exp, multiplier) = lookup(unit)
        Some(BigInt(amnt) * BigInt(multiplier).pow(exp))
      case Some(m) =>
        throw new RuntimeException(s"Invalid value \"$m\" as memory computational requirement.")
      case None => None
      case _ => ???
    }
  }
}