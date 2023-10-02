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

package io.viash.helpers.status

// object BuildStatus extends Enumeration {
//   type BuildStatus = Value
//   val ParseError, Disabled, BuildError, TestError, TestMissing, Success = Value
// }

sealed trait Status {
  val isError: Boolean
  val color: String
}
sealed trait ErrorStatus extends Status {
  val isError = true
  val color = Console.RED
}
sealed trait WarningStatus extends Status {
  val isError = false
  val color = Console.YELLOW
}
sealed trait SuccessStatus extends Status {
  val isError = false
  val color = Console.GREEN
}

case object ParseError extends ErrorStatus
case object Disabled extends WarningStatus
case object DependencyError extends ErrorStatus
case object MissingRunnerOrEngine extends WarningStatus
case object BuildError extends ErrorStatus
case object SetupError extends ErrorStatus
case object PushError extends ErrorStatus
case object TestError extends ErrorStatus
case object TestMissing extends WarningStatus
case object Success extends SuccessStatus
