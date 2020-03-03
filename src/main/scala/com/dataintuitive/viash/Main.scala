package com.dataintuitive.viash

import org.rogach.scallop._

import scala.io.Source

import io.circe.yaml.parser
import io.circe.yaml.syntax._

import cats.syntax.either._
import io.circe._
import io.circe.generic.auto._
import io.circe.syntax._

import yamusca.imports._, yamusca.implicits._

class Conf(arguments: Seq[String]) extends ScallopConf(arguments) {
  val mode = trailArg[String](default = Some("test"))
  val pSpec = opt[String](default = Some("portash.yaml"), descr = "Portash spec (default portash.yaml)")
  val vSpec = opt[String](default = Some("viash.yaml"), descr = "Viash spec (default viash.yaml)")
  verify()
}

case class Parameter(name: String, value: String)
case class Portash(name: String, command: String, parameters: Option[Seq[Parameter]], files: Option[Seq[String]])

object Main {
  def main(args: Array[String]) {
    val conf = new Conf(args)
    println("mode  = " + conf.mode())
    println("pSpec = " + conf.pSpec())
    println("vSpec = " + conf.vSpec())

    val pSpecStr = Source.fromFile(conf.pSpec()).mkString
    val vSpecStr = Source.fromFile(conf.vSpec()).mkString

    val template = Source.fromFile("templates/portash_to_script.template").mkString

    // Yamusca
    val json = parser.parse(pSpecStr)
    implicit val dataConv: ValueConverter[Portash] = ValueConverter.deriveConverter[Portash]

    println(json.toString())

    val p = json.leftMap(err => err: Error).flatMap(_.as[Portash]).valueOr(throw _)

    println(p.toString())

    val multilineString = """
    |This is a multiline string
    |In order to see if this is added
    |nicely to the YAML spec.
    """.stripMargin.trim

    val pp = p.copy(files = Some(Seq(multilineString)))

    val yaml = pp.asJson.asYaml

    // This works well for the multiline string above. Less so for simple strings on one line... overkill for that...
    // So we should be able to make a distinction here.
    println(
      io.circe.yaml.Printer(
          dropNullKeys = true,
          mappingStyle = io.circe.yaml.Printer.FlowStyle.Block,
          stringStyle = io.circe.yaml.Printer.StringStyle.Literal)
        .pretty(pp.asJson)
    )

    // Now, this _is_ possible if we apply different _printers_ for different parts of the yaml content.

  }
}
