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

import com.dataintuitive.viash.functionality._
import com.dataintuitive.viash.functionality.resources._
import com.dataintuitive.viash.functionality.dataobjects._
import com.dataintuitive.viash.config.Version
import com.dataintuitive.viash.helpers.{Docker, Bash}
import com.dataintuitive.viash.helpers.Circe._

/**
 * Next-gen Platform class for generating NextFlow (DSL2) modules.
 */
case class NextFlowPlatformPoc(
  id: String = "nextflowpoc",
  oType: String = "nextflowpoc"
) extends Platform {


  def modifyFunctionality(functionality: Functionality): Functionality = {
    val mainFile = PlainFile(
      dest = Some("main.nf"),
      text = Some("foo")
    )
    val nextflowConfigFile = createNextflowConfigFile(functionality)

    // remove main
    val otherResources = functionality.resources.filter(Some(_) == functionality.mainScript)

    functionality.copy(
      resources = mainFile :: nextflowConfigFile :: otherResources
    )
  }

  def renderNextflowConfig(functionality: Functionality): String = {
    val versStr = functionality.version.map(ver => s"\n  version = '$ver'").getOrElse("")
    val descStr = functionality.description.map(ver => s"\n  description = '$ver'").getOrElse("")
    val authStr = if (functionality.authors.isEmpty) "" else "\n  author = '" + functionality.authors.mkString(", ") + "'"
    s"""manifest {
    |  name = '${functionality.name}'
    |  mainScript = 'main.nf'$versStr$descStr$authStr
    |}
    |""".stripMargin
  }

  def createNextflowConfigFile(functionality: Functionality): Resource = {
    PlainFile(
      dest = Some("nextflow.config"),
      text = Some(renderNextflowConfig(functionality))
    )
  }
}

// vim: tabstop=2:softtabstop=2:shiftwidth=2:expandtab
