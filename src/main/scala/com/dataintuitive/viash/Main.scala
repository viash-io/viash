package com.dataintuitive.viash

import functionality._
import targets._

import java.nio.file.{Paths, Files}
import scala.io.Source

import java.nio.charset.StandardCharsets

object Main {
  def main(args: Array[String]) {
    val conf = new CLIConf(args)

    val funcPath = new java.io.File(conf.functionality())
    val targPath = new java.io.File(conf.platform())

    val functionality = Functionality.parse(funcPath)
    functionality.rootDir = funcPath

    val target = Target.parse(targPath)

    val fun2 = target.modifyFunctionality(functionality)

    conf.subcommand match {
      case Some(conf.run) => {
        val dir = Files.createTempDirectory("viash_" + fun2.name).toFile()
        writeResources(fun2.resources, funcPath, dir)
      }
      case Some(conf.export) => {
        val dir = new java.io.File(conf.export.output())
        dir.mkdirs()
        writeResources(fun2.resources, funcPath, dir)
      }
      case Some(_) => println("??")
      case None => println("No subcommand was specified")
    }
  }

  def writeResources(
    resources: Seq[Resource],
    inputDir: java.io.File,
    outputDir: java.io.File,
    overwrite: Boolean = true
  ) {
    // copy all files
    resources.foreach(
      resource => {
        val dest = Paths.get(outputDir.getAbsolutePath, resource.name)

        val destFile = dest.toFile()
        if (overwrite && destFile.exists()) {
          destFile.delete()
        }

        if (resource.path.isDefined) {
          val sour = Paths.get(inputDir.getParent(), resource.path.get)
          Files.copy(sour, dest)
        } else {
          val code = resource.code.get
          Files.write(dest, code.getBytes(StandardCharsets.UTF_8))
        }

        destFile.setExecutable(resource.isExecutable)
      }
    )
  }

  def run(

  ) = {
//      import sys.process._
//
//      command !
  }
}
