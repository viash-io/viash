package com.dataintuitive.viash.helpers

import com.dataintuitive.viash.functionality._
import com.dataintuitive.viash.functionality.resources._
import java.nio.file.Paths
import scala.io.Source

object BashHelper {
  private def readUtils(s: String) = {
    val path = s"com/dataintuitive/viash/helpers/bashutils/$s.sh"
    Source.fromResource(path).getLines().mkString("\n")
  }

  lazy val ViashQuote = readUtils("ViashQuote")
  lazy val ViashExtractFlags = readUtils("ViashExtractFlags")
  lazy val ViashRemoveFlags = readUtils("ViashRemoveFlags")
  lazy val ViashAbsolutePath = readUtils("ViashAbsolutePath")
  lazy val ViashAutodetectMount = readUtils("ViashAutodetectMount")

  // usage: `ViashSourceDir ${BASH_SOURCE[0]}`
  lazy val ViashSourceDir = readUtils("ViashSourceDir")

  def save(saveVariable: String, args: Seq[String]) = {
    saveVariable + "=\"$" + saveVariable + " " + args.mkString(" ") + "\""
  }

  // generate strings in the form of:
  // SAVEVARIABLE="$SAVEVARIABLE $(Quote arg1) $(Quote arg2)"
  def quoteSave(saveVariable: String, args: Seq[String]) = {
    saveVariable + "=\"$" + saveVariable +
      args.map(" $(ViashQuote \"" + _ + "\")").mkString +
      "\""
  }

  def argStore(name: String, plainName: String, store: String, argsConsumed: Int, storeUnparsed: Option[String]) = {
    val passStr =
      if (storeUnparsed.isDefined) {
        "\n            " + quoteSave(storeUnparsed.get, (1 to argsConsumed).map("$" + _))
      } else {
        ""
      }
    s"""         $name)
      |            ${plainName}=$store$passStr
      |            shift $argsConsumed
      |            ;;""".stripMargin
  }
  def argStoreSed(name: String, plainName: String, storeUnparsed: Option[String]) = {
    argStore(name + "=*", plainName, "$(ViashRemoveFlags \"$1\")", 1, storeUnparsed)
  }

}
