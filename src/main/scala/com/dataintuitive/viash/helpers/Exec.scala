package com.dataintuitive.viash.helpers

import sys.process.Process
import java.io.File

object Exec {
  def run(commands: Seq[String], cwd: Option[File] = None, extraEnv: Seq[(String, String)] = Nil) = {
    try {
      Process(commands, cwd = cwd, extraEnv = extraEnv: _*).!!
    } catch {
      case e: Throwable => {
        println(e.getMessage)
        throw e
      }
    }
  }

  def appendToEnv(key: String, value: String) = util.Properties.envOrNone(key) match {
    case Some(v) if v.nonEmpty => s"$v${System getProperty "path.separator"}$value"
    case _ => value
  }
}
