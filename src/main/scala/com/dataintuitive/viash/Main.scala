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
    funcPath: java.io.File,
    dir: java.io.File
  ) = {
    
    functionality.resources.foreach(
      resource => {
        // todo: copy file instead of reading it if it is not the main code file
        
        val code = 
          if (resource.path.isDefined) {
            val path = Paths.get(funcPath.getParent(), resource.path.get).toFile()
            Source.fromFile(path).mkString
          } else {
            resource.code.get
          }
        
        // do something with code if name starts with 'main'
        
        val pathOut = Paths.get(dir.getAbsolutePath, resource.name)     
        Files.write(pathOut, code.getBytes(StandardCharsets.UTF_8))
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
