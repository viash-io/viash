package io.viash.helpers

import scala.io.Source

object Resources {
  def read(path: String): String = {
    Source.fromResource(s"io/viash/$path").getLines().mkString("\n")
  }
}
