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
import com.dataintuitive.viash.functionality.dataobjects._
import java.nio.file.Path
import java.nio.file.Paths

case class DebugPlatform(
  id: String = "debug",
  oType: String = "debug",
  path: String
) extends Platform {
  def modifyFunctionality(functionality: Functionality): Functionality = {
    val executor = s"tee '$path' > /dev/null"

    val newFun = functionality.copy(
      arguments = functionality.arguments.map {
        case arg if arg.required && arg.default.isDefined => 
          arg.copyDO(required = false)
        case arg: BooleanObject if arg.default.isEmpty => 
          arg.copyDO(required = false, default = Some(true))
        case arg: DoubleObject if arg.default.isEmpty => 
          arg.copyDO(required = false, default = Some(123.0))
        case arg: FileObject if arg.default.isEmpty => 
          arg.copyDO(required = false, default = Some(Paths.get("/path/to/file")))
        case arg: IntegerObject if arg.default.isEmpty =>
           arg.copyDO(required = false, default = Some(123))
        case arg: StringObject if arg.default.isEmpty => 
          arg.copyDO(required = false, default = Some("value"))
        case a => a
      }
    )

    // create new bash script
    val bashScript = BashScript(
      dest = Some(functionality.name),
      text = Some(BashWrapper.wrapScript(
        executor = executor,
        functionality = newFun
      ))
    )

    newFun.copy(
      resources = Some(List(bashScript))
    )
  }
}
