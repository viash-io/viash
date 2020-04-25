package com.dataintuitive.viash

import java.io.File
import sys.process.Process

object Exec {
  def run(commands: Seq[String], path: File) = {
    try {
      Process(commands).!!
      //Process(commands, path).!!
    } catch {
      case e: Throwable => {
        println(e.getMessage)
        throw e
      }
    }
  }
}

import org.scalatest.Tag

object DockerTest extends Tag("com.dataintuitive.viash.DockerTest")