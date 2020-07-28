package com.dataintuitive.viash.meta

import io.circe.{ Decoder, Encoder, Json }
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

import com.dataintuitive.viash.functionality.resources.PlainFile
import com.dataintuitive.viash.functionality._
import com.dataintuitive.viash.platforms._
import io.circe.yaml.Printer

case class Meta(
  version: String,
  fun: Functionality,
  platform: Platform,
  functionalityPath: String,
  platformPath: String,
  outputPath: String,
  executablePath: String
  ) {
    def info =
      s"""version: ${version}
         |functionalityPath: ${functionalityPath}
         |platformPath: ${platformPath}
         |outputPath: ${outputPath}
         |executablePath: ${executablePath}""".stripMargin

    def yaml = {

      val strippedMeta = StrippedMeta(
        version,
        functionalityPath,
        platformPath,
        outputPath,
        executablePath
      )

      val encoded = encodeNested(Nested(strippedMeta, fun, platform))

      // Options: https://github.com/circe/circe-yaml/blob/master/src/main/scala/io/circe/yaml/Printer.scala
      val pretty = Printer(
        dropNullKeys = true,
        mappingStyle = Printer.FlowStyle.Block,
        splitLines = true
      ).pretty(encoded)

      pretty
    }

    val resource = PlainFile(Some("viash.yaml"), None, Some(yaml))
  }
