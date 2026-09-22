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

package io.viash.wrapper

import io.viash.TestHelper
import io.viash.helpers.Logger
import org.scalatest.funsuite.AnyFunSuite

class BashWrapperPipeTest extends AnyFunSuite {
  Logger.UseColorOverride.value = Some(false)

  test("generated executable should not mangle a script line that starts with '||'") {
    // regression test for https://github.com/viash-io/viash/issues/908
    val configFile = getClass.getResource("/testbash/pipe_char_in_script/config.vsh.yaml").getPath

    val testOutput = TestHelper.testMain(
      "run",
      configFile
    )

    assert(testOutput.stdout.contains("exit code: 1"))
  }
}
