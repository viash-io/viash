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

import sys.process.{Process, ProcessLogger}
import java.io.{ByteArrayOutputStream, File, PrintWriter}
import java.nio.file.Path

object Exec {

  case class ExecOutput(
    command: Seq[String],
    exitValue: Int,
    output: String
  )

  def run(command: Seq[String], cwd: Option[File] = None, extraEnv: Seq[(String, String)] = Nil): String = {
    Process(command, cwd = cwd, extraEnv = extraEnv: _*).!!
  }

  def run2(command: Seq[String], cwd: Option[File] = None, extraEnv: Seq[(String, String)] = Nil, loggers: Seq[String => Unit] = Nil): ExecOutput = {
    // run command, collect output
    val stream = new ByteArrayOutputStream
    val printwriter = new PrintWriter(stream)

    val logger = (s: String) => {
      printwriter.println(s)
      for (log <- loggers) log(s)
    }

    // run command, collect output
    try {
      val exitValue = Process(command, cwd = cwd, extraEnv = extraEnv: _*).!(ProcessLogger(logger, logger))
      printwriter.flush()
      ExecOutput(command, exitValue, stream.toString)
    } finally {
      printwriter.close()
    }
  }

  def runPath(command: Seq[String], cwd: Option[Path] = None, extraEnv: Seq[(String, String)] = Nil): String = {
    run(command, cwd.map(_.toFile), extraEnv)
  }

  def run2Path(command: Seq[String], cwd: Option[Path] = None, extraEnv: Seq[(String, String)] = Nil, loggers: Seq[String => Unit] = Nil): ExecOutput = {
    run2(command, cwd.map(_.toFile), extraEnv, loggers)
  }
}
