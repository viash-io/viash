package com.dataintuitive.viash.config

import com.dataintuitive.viash.functionality._
import com.dataintuitive.viash.platforms._
import com.dataintuitive.viash.helpers.IOHelper
import java.net.URI
import io.circe.yaml.parser

case class Config(
  functionality: Functionality,
  platform: Option[Platform] = None,
  platforms: List[Platform] = Nil
)

object Config {
  def parse(uri: URI): Config = {
    val str = IOHelper.read(uri)
    parse(str, uri)
  }

  def parse(yamlText: String, uri: URI): Config = {
    val config = parser.parse(yamlText)
      .fold(throw _, _.as[Config])
      .fold(throw _, identity)

    val fun = config.functionality

    val resources = fun.resources.getOrElse(Nil).map(Functionality.makeResourcePathAbsolute(_, uri))
    val tests = fun.tests.getOrElse(Nil).map(Functionality.makeResourcePathAbsolute(_, uri))

    config.copy(
      functionality = fun.copy(
        resources = Some(resources),
        tests = Some(tests)
      )
    )
  }
}