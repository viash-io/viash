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
    val platPath = new java.io.File(conf.platform())
    
    
    
    val functionality = Functionality.parse(funcPath)
    val platform = Target.parse(platPath)
    
    val resources = 
      functionality.resources.toList ::: 
      platform.setupResources(functionality).toList
    
    conf.subcommand match {
      case Some(conf.run) => {
        val dir = Files.createTempDirectory("viash_" + functionality.name).toFile()
        writeResources(resources, funcPath, dir)
      }
      case Some(conf.export) => {
        val dir = new java.io.File(conf.export.output())
        dir.mkdirs()
        writeResources(resources, funcPath, dir)
      }
      case Some(_) => println("??")
      case None => println("No subcommand was specified")
    }
  }
  
  def writeResources(
    resources: Seq[Resource],
    inputDir: java.io.File,
    outputDir: java.io.File
  ) {
    // copy all files
    resources.foreach(
      resource => {
        val dest = Paths.get(outputDir.getAbsolutePath, resource.name)
         
        if (resource.path.isDefined) {
          val sour = Paths.get(inputDir.getParent(), resource.path.get)
          Files.copy(sour, dest)
        } else {
          val code = resource.code.get
          Files.write(dest, code.getBytes(StandardCharsets.UTF_8))
        }
      
      }
    )
  }
  
  def processMain(
    functionality: Functionality,
    platform: Target, 
    dir: java.io.File,
  ) = {
    
  }
  
  def run(
      
  ) = {
//      import sys.process._
//      
//      command !
  }
}
