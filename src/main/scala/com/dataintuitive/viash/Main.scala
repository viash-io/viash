package com.dataintuitive.viash

import org.rogach.scallop.ScallopConf

import scala.io.Source

import io.circe.yaml.parser
import io.circe.yaml.syntax._

import cats.syntax.either._
import io.circe._
import io.circe.generic.auto._
import io.circe.syntax._

import functionality._

class Conf(arguments: Seq[String]) extends ScallopConf(arguments) {
  val mode = trailArg[String](default = Some("test"))
  val pSpec = opt[String](default = Some("portash.yaml"), descr = "Portash spec (default portash.yaml)")
  val vSpec = opt[String](default = Some("viash.yaml"), descr = "Viash spec (default viash.yaml)")
  verify()
}


object Main {
  def main(args: Array[String]) {
    val conf = new Conf(args)
    println(s"mode = ${conf.mode()}, pSpec = ${conf.pSpec()}, vSpec = ${conf.vSpec()}")
    println()

    val pSpecStr = Source.fromFile(conf.pSpec()).mkString
    val vSpecStr = Source.fromFile(conf.vSpec()).mkString
    
    val Right(json) = parser.parse(pSpecStr)
    
    val Right(fun) = json.as[Functionality]
    
    import io.circe.yaml._

    println(
      Printer(dropNullKeys = true, mappingStyle = Printer.FlowStyle.Block)
        .pretty(json)
    )
  }
}
