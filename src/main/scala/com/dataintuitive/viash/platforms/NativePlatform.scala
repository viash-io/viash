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
import com.dataintuitive.viash.functionality.resources._
import com.dataintuitive.viash.platforms.requirements._
import com.dataintuitive.viash.config.Version
import com.dataintuitive.viash.wrapper.BashWrapper

case class NativePlatform(
  id: String = "native",
  setup: List[Requirements] = Nil
) extends Platform {
  val `type` = "native"

  val requirements: List[Requirements] = setup

  def modifyFunctionality(functionality: Functionality): Functionality = {
    val executor = functionality.mainScript match {
      case None => "eval"
      case Some(_: Executable) => "eval"
      case Some(_) => "bash"
    }

    // create new bash script
    val bashScript = BashScript(
      dest = Some(functionality.name),
      text = Some(BashWrapper.wrapScript(
        executor = executor,
        functionality = functionality,
        setupCommands = setupCommands
      ))
    )

    functionality.copy(
      resources = Some(bashScript :: functionality.resources.getOrElse(Nil).tail)
    )
  }

  def setupCommands: String = {
    val runCommands = requirements.map(_.installCommands)

    val commands =
      runCommands.map(li =>
        if (li.isEmpty) {
          ""
        } else {
          "cat << 'HERE'\n" +
          "# run the following commands:\n" +
          li.mkString("", " && \\\n  ", "\n") +
          "HERE\n"
        }
      ).mkString

    s"""function ViashSetup {
       |${if (commands == "") ":\n" else commands}}""".stripMargin
  }
}
