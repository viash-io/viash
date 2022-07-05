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

import com.dataintuitive.viash.config.Config
import com.dataintuitive.viash.functionality.Functionality
import com.dataintuitive.viash.functionality.resources._
import com.dataintuitive.viash.platforms.requirements._
import com.dataintuitive.viash.config.Version
import com.dataintuitive.viash.wrapper.BashWrapper
import com.dataintuitive.viash.helpers.description
import com.dataintuitive.viash.helpers.example

@description("""Running a Viash component on a native platform means that the script will be executed in your current environment.
               |Any dependencies are assumed to have been installed by the user, so the native platform is meant for developers (who know what they’re doing) or for simple bash scripts (which have no extra dependencies).
               |""".stripMargin)
case class NativePlatform(
  @description("As with all platforms, you can give a platform a different name. By specifying `id: foo`, you can target this platform (only) by specifying `-p foo` in any of the Viash commands.")
  @example("id: foo", "yaml")
  id: String = "native",
  `type`: String = "native"
) extends Platform {
  def modifyFunctionality(config: Config, testing: Boolean): Functionality = {
    val functionality = config.functionality
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
        functionality = functionality
      ))
    )

    functionality.copy(
      resources = bashScript :: functionality.resources.tail
    )
  }
}
