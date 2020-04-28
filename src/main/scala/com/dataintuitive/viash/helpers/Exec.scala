package com.dataintuitive.viash.helpers

import sys.process.Process

object Exec {
  def run(commands: Seq[String]) = {
    try {
      Process(commands).!!
    } catch {
      case e: Throwable => {
        println(e.getMessage)
        throw e
      }
    }
  }
}