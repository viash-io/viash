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

package io.viash.platforms

import io.viash.schemas._

@description(
  """Running a Viash component on a native platform means that the script will be executed in your current environment.
    |Any dependencies are assumed to have been installed by the user, so the native platform is meant for developers (who know what they're doing) or for simple bash scripts (which have no extra dependencies).
    |""")
@example(
  """platforms:
    |  - type: native
    |""",
  "yaml")
@deprecated("Use 'engines' and 'runners' instead.", "0.9.0", "0.10.0")
@subclass("native")
case class NativePlatform(
  @description("As with all platforms, you can give a platform a different name. By specifying `id: foo`, you can target this platform (only) by specifying `-p foo` in any of the Viash commands.")
  @example("id: foo", "yaml")
  @default("native")
  id: String = "native",
  `type`: String = "native"
) extends Platform