package com.dataintuitive.viash

import java.io.File
import java.nio.file.Paths

import com.dataintuitive.viash.config._
import com.dataintuitive.viash.helpers.IO

import scala.sys.process.{Process, ProcessLogger}

object ViashRun {
  def apply(config: Config, args: Seq[String], keepFiles: Boolean) {
    val fun = config.functionality
    val dir = IO.makeTemp("viash_" + fun.name)

    // execute command, print everything to console
    try {
      // write executable and resources to temporary directory
      IO.writeResources(fun.resources.getOrElse(Nil), dir)

      // determine command
      val cmd =
        Array(Paths.get(dir.toString, fun.name).toString) ++ args

      // execute command, print everything to console
      val code = Process(cmd).!(ProcessLogger(println, println))
      System.exit(code)
    } finally {
      // remove tempdir if desired
      if (!keepFiles) {
        IO.deleteRecursively(dir)
      } else {
        println(s"Files and logs are stored at '$dir'")
      }
    }
  }
}
