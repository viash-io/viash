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

package io.viash.config.resources

import io.viash.schemas._

import java.net.URI
import io.viash.languages.{Bash => BashLang}

@description("""An executable Bash script.
               |When defined in resources, only the first entry will be executed when running the built component or when running `viash run`.
               |When defined in test_resources, all entries will be executed during `viash test`.""")
@subclass("bash_script")
case class BashScript(
  path: Option[String] = None,
  text: Option[String] = None,
  dest: Option[String] = None,
  is_executable: Option[Boolean] = Some(true),
  parent: Option[URI] = None,

  @description("""Whether to use jq for JSON parameter parsing and store multiple-value arguments as Bash arrays.
               |  - `true`: Use jq for JSON parsing. Arguments with `multiple: true` are stored as Bash arrays (e.g. `par_input=("a" "b" "c")`). Requires jq to be installed.
               |  - `false`: Use the built-in JSON parser. Arguments with `multiple: true` are stored as separator-delimited strings (e.g. `par_input="a;b;c"`), using the argument's `multiple_sep` (default `";"`).
               |  - Not specified (default): Same behavior as `false`, but a deprecation warning is shown at build time indicating that the default will change to `true` in a future version of Viash.""")
  @example("use_jq: true", "yaml")
  @since("Viash 0.10.0")
  use_jq: Option[Boolean] = None,

  @description("Specifies the resource as a Bash script.")
  `type`: String = "bash_script"
) extends Script {
  val language = BashLang
  def copyResource(path: Option[String], text: Option[String], dest: Option[String], is_executable: Option[Boolean], parent: Option[URI]): Resource = {
    copy(path = path, text = text, dest = dest, is_executable = is_executable, parent = parent)
  }
}
