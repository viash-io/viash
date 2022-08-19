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

import scala.io.Source

object NextflowHelper {
  private def readSource(s: String) = {
    val path = s"io/viash/platforms/nextflow/$s"
    Source.fromResource(path).getLines().mkString("\n")
  }

  lazy val vdsl3Helper: String = readSource("VDSL3Helper.nf")
  lazy val workflowHelper: String = readSource("WorkflowHelper.nf")
  lazy val profilesHelper: String = readSource("ProfilesHelper.config")
}
