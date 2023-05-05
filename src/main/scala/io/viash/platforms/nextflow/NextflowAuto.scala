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

package io.viash.platforms.nextflow

case class NextflowAuto(
  simplifyInput: Boolean = true,
  simplifyOutput: Boolean = true,
  transcript: Boolean = false,
  publish: Boolean = false,
  labels: Map[String, String] =
    Seq(1, 2, 5, 10, 20, 50, 100, 200, 500).map(s => (s"mem${s}gb", s"memory = ${s}.Gb")).toMap ++
    Seq(1, 2, 5, 10, 20, 50, 100).map(s => (s"cpu$s", s"cpus = $s"))
)