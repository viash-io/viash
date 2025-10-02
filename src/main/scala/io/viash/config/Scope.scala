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

import io.viash.schemas.description

enum ScopeEnum {
  case Test, Private, Public
}

@description(
  """Defines the scope of the component.
    |
    |- `test`: only available during testing; components aren't published
    |- `private`: only meant for internal use within a workflow or other component
    |- `public`: core component or workflow meant for general use""")
case class Scope(
  @description(
    """Defines whether the image is published or not.
      |
      |- `test`: image is only used during testing and is transient
      |- `private`: image is published in the registry
      |- `public`: image is published in the registry""")
  image: ScopeEnum,
  @description(
    """Defines the target location of the component.
      |
      |- `test`: target folder is only used during testing and is transient
      |- `private`: target folder can be published in target/_private or target/dependencies/_private
      |- `public`: target is published in target/executable or target/nextflow""")
  target: ScopeEnum,
)

object Scope {
  def apply(scopeValue: ScopeEnum): Scope = {
    Scope(scopeValue, scopeValue)
  }
}
