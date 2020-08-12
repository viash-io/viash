package com.dataintuitive.viash.meta

import io.circe.{ Decoder, Encoder, Json }
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

import sys.process._

import com.dataintuitive.viash.functionality.resources.PlainFile
import com.dataintuitive.viash.functionality._
import com.dataintuitive.viash.platforms._
import io.circe.yaml.Printer

case class Meta(
  viash_version: String,
  fun: Functionality,
  platform: Platform,
  functionality_path: String,
  platform_path: String,
  output_path: String,
  executable_path: String
) {

    val isGitRepo = scala.util.Try("git rev-parse --is-inside-work-tree" !!)
      .map(_.trim.toBoolean)
      .toOption
      .getOrElse(false)

    val localGitRepo = scala.util.Try("git rev-parse --show-toplevel" !!)
      .map(_.trim)
      .toOption
      .getOrElse("NA")

    val remoteGitRepo = scala.util.Try("git remote --verbose" !!)
      .toOption
      .map(_.split("\n").map(_.split("\\s")).filter(_.headOption.getOrElse("NA") contains "origin").headOption.getOrElse("No remote configured").toString)
      .getOrElse("NA")

    val commit = scala.util.Try("git log --oneline" !!)
      .toOption
      .map(_.split("\n").head.split(" ").head)
      .getOrElse("NA")

    def info =
      s"""viash_version:      ${viash_version}
         |functionality path: ${functionality_path}
         |platform path:      ${platform_path}
         |output path:        ${output_path}
         |executable path:    ${executable_path}
         |remote git repo:    ${remoteGitRepo}""".stripMargin

    def yaml = {

      val strippedMeta = StrippedMeta(
        viash_version,
        remoteGitRepo,
        commit,
        functionality_path,
        platform_path,
        output_path,
        executable_path
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
