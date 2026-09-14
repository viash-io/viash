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

package io.viash.runners.nextflow

import io.viash.config.Config
import io.viash.config.resources.BashScript
import io.viash.helpers.Logger
import org.scalatest.funsuite.AnyFunSuite

class NextflowHelperTest extends AnyFunSuite {
  Logger.UseColorOverride.value = Some(false)

  test("generateScriptStr should not mangle a script line that starts with '||'") {
    // regression test for https://github.com/viash-io/viash/issues/908
    // a line starting with '|' (after whitespace) used to be misinterpreted
    // as a stripMargin delimiter, corrupting the generated rawScript.
    val script =
      "false \\\n" +
      "|| echo_exit_code=\"$?\"\n" +
      "echo \"exit code: $echo_exit_code\"\n"

    val config = Config(
      name = "myscript",
      resources = List(BashScript(text = Some(script), dest = Some("script.sh")))
    )

    val rawScript = NextflowHelper.generateScriptStr(config)

    assert(rawScript.contains("|| echo_exit_code="))
  }
}
