package com.dataintuitive.viash

import functionality._
import platform._

object Main {
  def main(args: Array[String]) {
    val conf = new CLIConf(args)
    
    println(s"functionality = ${conf.functionality()}, platform = ${conf.platform()}")
    
    val functionality = Functionality.parse(conf.functionality())
    val platform = Platform.parse(conf.platform())
    
    conf.subcommand match {
      case Some(conf.run) => {
        println(s"Subcommand 'run'. output: ${conf.run.test()}")
        val dir = java.nio.file.Files.createTempDirectory("viash_" + functionality.name).toFile()
        
        export(functionality, platform, dir)
      }
      case Some(conf.export) => {
        println(s"Subcommand 'export'. output: ${conf.export.output()}")
        val dir = new java.io.File(conf.export.output())
        dir.mkdirs()
        
        export(functionality, platform, dir)
      }
      case Some(_) => println("??")
      case None => println("No subcommand was specified")
    }
  }
  
  def export(functionality: Functionality, platform: Platform, dir: java.io.File) = {
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
