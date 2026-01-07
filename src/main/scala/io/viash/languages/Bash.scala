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

package io.viash.languages

import io.viash.helpers.Resources

object Bash extends Language {
  val id: String = "bash"
  val name: String = "Bash"
  val extensions: Seq[String] = Seq(".sh")
  val commentStr: String = "#"
  val executor: Seq[String] = Seq("bash")
  val viashParseYamlCode: String = Resources.read("languages/bash/ViashParseYaml.sh")
  val viashParseJsonCode: String = Resources.read("languages/bash/ViashParseJson.sh")
}
