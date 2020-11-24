package com.dataintuitive.viash

import java.nio.file.Paths

import com.dataintuitive.viash.config._
import com.dataintuitive.viash.functionality.dataobjects.{FileObject, Output}
import com.dataintuitive.viash.helpers.IO

import scala.sys.process.{Process, ProcessLogger}

object ViashRun {
  def apply(config: Config, args: Seq[String], keepFiles: Option[Boolean]) {
    val fun = config.functionality
    val dir = IO.makeTemp("viash_" + fun.name)

    val dirArg = FileObject(
      name = "--viash_tempdir_arg",
      direction = Output,
      default = Some(dir)
    )
    val fun2 = fun.copy(
      dummy_arguments = Some(List(dirArg))
    )

    // execute command, print everything to console
    var code = -1
    try {
      // write executable and resources to temporary directory
      IO.writeResources(fun2.resources.getOrElse(Nil), dir)

      // determine command
      val cmd =
        Array(Paths.get(dir.toString, fun2.name).toString) ++ args

      // execute command, print everything to console
      code = Process(cmd).!(ProcessLogger(println, println))
      System.exit(code)
    } finally {
      // remove tempdir if desired
      if (!keepFiles.getOrElse(code != 0)) {
        IO.deleteRecursively(dir)
      } else {
        println(s"Files and logs are stored at '$dir'")
      }
    }
  }
}
