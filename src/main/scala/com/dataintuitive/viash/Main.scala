package com.dataintuitive.viash

import org.rogach.scallop.{ScallopConf, Subcommand}
import scala.io.Source
import io.circe.yaml.parser
import functionality._

class Conf(arguments: Seq[String]) extends ScallopConf(arguments) {
  val functionality = opt[String](
    default = Some("functionality.yaml"), 
    descr = "Path to the functionality specifications (default functionality.yaml)",
    required = true
  )
  val platform = opt[String](
    default = Some("platform.yaml"), 
    descr = "Path to the platform specifications (default platform.yaml)",
    required = true
  )
  
  val run = new Subcommand("run") {
    val test = opt[String](
      descr = "Test argument"
    )
  }
  val export = new Subcommand("export") {
    val output = opt[String](
      default = Some("executable.sh"), 
      descr = "Path to output executable.",
      required = true
    )
  }
  
  addSubcommand(run)
  addSubcommand(export)
  
  verify()
}




object Main {
  def parseFunctionality(filename: String): Functionality = {
    val str = Source.fromFile(filename).mkString
    val json = parser.parse(str)
    val fun = json match {
      case Right(js) => js.as[Functionality]
      case Left(e) => throw e
    }
    fun match {
      case Right(f) => f
      case Left(e) => throw e
    }
  }
  
  def parsePlatform(filename: String): Unit = {
//    val str = Source.fromFile(filename).mkString
//    val json = parser.parse(str)
//    val fun = json match {
//      case Right(js) => js.as[Functionality]
//      case Left(e) => throw e
//    }
//    fun match {
//      case Right(f) => f
//      case Left(e) => throw e
//    }
  }
    
  def main(args: Array[String]) {
    val conf = new Conf(args)
    
    println(s"functionality = ${conf.functionality()}, platform = ${conf.platform()}")
    
    conf.subcommand match {
      case Some(conf.run) => println(s"Subcommand 'run'. output: ${conf.run.test()}")
      case Some(conf.export) => println(s"Subcommand 'export'. output: ${conf.export.output()}")
      case Some(_) => println("??")
      case None => println("No subcommand was specified")
    }
    
//    println(conf.subcommand.get)
//    println(conf.export.verified)
//    println(conf.run.verified)
//    println(s"export.output = ${conf.export.output()}")
//    println(s"run.test = ${conf.run.test}")

    val functionality = parseFunctionality(conf.functionality())
    val platform = parsePlatform(conf.platform())
    
    
  }
}
