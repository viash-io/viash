package com.dataintuitive.viash

import java.io.File
import sys.process.Process

object Exec {
  def run(commands: Seq[String], path: File) = {
    try {
      Process(commands, path).!!
    } catch {
      case e: Throwable => {
        println(e.getMessage)
        throw e
      }
    }
  }
  
  def dockerAvailable = {
    try {
      Process(Array("docker", "--help")).!!
      true
    } catch {
      case e: Throwable => {
        false
      }
    }
  }
}

import org.scalatest.Tag

object DockerTest extends Tag("com.dataintuitive.viash.DockerTest")