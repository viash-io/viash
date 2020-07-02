package com.dataintuitive.viash.functionality.resources

import com.dataintuitive.viash.functionality.Functionality
import java.nio.file.{Path, Files}
import java.nio.charset.StandardCharsets

trait Script extends Resource {
  def command(script: String): String
  def commandSeq(script: String): Seq[String]

  def generateArgparse(functionality: Functionality): String

  val commentStr: String

  def readWithArgparse(implicit functionality: Functionality): Option[String] = {
    read.map(code => {
      val lines = code.split("\n")

      val startIndex = lines.indexWhere(_.contains("VIASH START"))
      val endIndex = lines.indexWhere(_.contains("VIASH END"))

      if (startIndex >= 0 && endIndex >= 0) {
        val li =
          lines.slice(0, startIndex + 1) ++
          Array(
            commentStr + " The following code has been auto-generated by Viash.",
            generateArgparse(functionality)
          ) ++
          lines.slice(endIndex, lines.length)

        li.mkString("\n")
      } else {
        code
      }
    })
  }

  def writeWithArgparse(path: Path, overwrite: Boolean)(implicit functionality: Functionality) {
    val code = readWithArgparse(functionality)
    if (code.isDefined) {
      val file = path.toFile()

      if (overwrite && file.exists()) {
        file.delete()
      }

      Files.write(path, code.get.getBytes(StandardCharsets.UTF_8))

      file.setExecutable(is_executable)
    }
  }
}
