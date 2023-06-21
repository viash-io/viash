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

package io.viash.helpers

import io.viash.schemas.description
import io.viash.schemas.documentFully

@description("Provides access to System Envirionment variables")
@documentFully
trait SysEnvTrait {
  @description("`VIASH_HOME` or else `HOME`/.viash")
  def viashHome: String
  @description("`VIASH_VERSION`")
  def viashVersion: Option[String]
}


object SysEnv extends SysEnvTrait {
  private val sysEnvOverride = scala.collection.mutable.Map.empty[String, String]

  private def get(key: String): Option[String] = sysEnvOverride.get(key) orElse sys.env.get(key)
  private def getOrElse(key: String, default: => String): String = get(key) match {
    case Some(v) => v
    case None => default
  }

  private lazy val codeRunInTestBench = { getClass.getPackage.getImplementationTitle == null }

  def set(key: String, value: String) = {
    if (!codeRunInTestBench)
      throw new IllegalAccessException("SysEnv.set is not allowed in production code.")
    sysEnvOverride.addOne(key -> value)
  }
  def remove(key: String) = {
    if (!codeRunInTestBench)
      throw new IllegalAccessException("SysEnv.remove is not allowed in production code.")
    sysEnvOverride.remove(key)
  }

  def viashHome = getOrElse("VIASH_HOME", sys.env("HOME") + "/.viash")
  def viashVersion = get("VIASH_VERSION")
} 
