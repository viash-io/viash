package com.dataintuitive.viash

import functionality._
import platform._

import java.nio.file.{Paths, Files}
import scala.io.Source

import java.nio.charset.StandardCharsets

object Main {
  def main(args: Array[String]) {
    val conf = new CLIConf(args)
    
    val funcPath = new java.io.File(conf.functionality())
    val platPath = new java.io.File(conf.platform())
    
    
    
    val functionality = Functionality.parse(funcPath)
    val platform = Platform.parse(platPath)
    
    conf.subcommand match {
      case Some(conf.run) => {
        val dir = Files.createTempDirectory("viash_" + functionality.name).toFile()
        export(functionality, platform, funcPath, dir)
      }
      case Some(conf.export) => {
        val dir = new java.io.File(conf.export.output())
        dir.mkdirs()
        export(functionality, platform, funcPath, dir)
      }
      case Some(_) => println("??")
      case None => println("No subcommand was specified")
    }
  }
  
  def export(
    functionality: Functionality,
    platform: Platform, 
    inputDir: java.io.File,
    outputDir: java.io.File
  ) = {
    
    functionality.resources.foreach(
      resource => {
        val sour = Paths.get(inputDir.getParent(), resource.path.get)
        val dest = Paths.get(outputDir.getAbsolutePath, resource.name)
         
          if (resource.path.isDefined) {
            val code = Source.fromFile(sour.toFile()).mkString
            Files.write(dest, code.getBytes(StandardCharsets.UTF_8))
          } else {
            Files.copy(sour, dest)
          }
        
        // do something with code if name starts with 'main'
        
        
      }
    )
//    dir.getPath()
//        val command = functionality.platform match {
//          case "R" => "Rscript main.R"
//          case "Python" => "python main.py"
//        }
//        
//        import sys.process._
//        
//        command !
  }
}
