package com.dataintuitive.viash.functionality

sealed trait Platform {
  def command(script: String): String
}

case object R extends Platform {
  def command(script: String) = {
    "Rscript " + script
  }
}

case object Python extends Platform {
  def command(script: String) = {
    "python " + script
  }
}

object Platform {
  def fromString(str: String) = {
    str match {
      case "R" => R
      case "Python" => Python
      case s => throw new RuntimeException(s"Unrecognised platform '${s}'.") 
    }
  }
}