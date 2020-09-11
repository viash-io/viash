package com.dataintuitive.viash.helpers

import sys.process.{Process, ProcessLogger}
import java.io.{ByteArrayOutputStream, PrintWriter, File}

object Exec {
  case class ExecOutput(command: Seq[String], exitValue: Int, output: String)

  def run(command: Seq[String], cwd: Option[File] = None, extraEnv: Seq[(String, String)] = Nil): String = {
    try {
      Process(command, cwd = cwd, extraEnv = extraEnv: _*).!!
    } catch {
      case e: Throwable =>
        println(e.getMessage)
        throw e
    }
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
      ExecOutput(command, exitValue, stream.toString)
    } finally {
      printwriter.close()
    }
  }
}
