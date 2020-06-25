package com.dataintuitive.viash.helpers

import sys.process.Process
import java.io.File
import java.nio.file.{Paths, Files}
import scala.reflect.io.Directory

import sys.process.{Process, ProcessLogger}
import java.io.{ByteArrayOutputStream, PrintWriter, FileWriter, File}
import java.nio.file.{Paths, Files}

case class ExecOutput(command: Seq[String], exitValue: Int, output: String)

object Exec {
  def run(command: Seq[String], cwd: Option[File] = None, extraEnv: Seq[(String, String)] = Nil) = {
    try {
      Process(command, cwd = cwd, extraEnv = extraEnv: _*).!!
    } catch {
      case e: Throwable => {
        println(e.getMessage)
        throw e
      }
    }
  }

  def run2(command: Seq[String], cwd: Option[File] = None, extraEnv: Seq[(String, String)] = Nil, loggers: Seq[String => Unit] = Nil) = {
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

  def appendToEnv(key: String, value: String) = util.Properties.envOrNone(key) match {
    case Some(v) if v.nonEmpty => s"$v${System getProperty "path.separator"}$value"
    case _ => value
  }

  def makeTemp(name: String) = {
    val tempdir = scala.util.Properties.envOrElse("VIASH_TEMP", "/tmp")
    val temp = Files.createTempDirectory(Paths.get(tempdir), name).toFile()
    temp.mkdirs()
    temp
  }

  def deleteRecursively(dir: File) {
    new Directory(dir).deleteRecursively()
  }
}
