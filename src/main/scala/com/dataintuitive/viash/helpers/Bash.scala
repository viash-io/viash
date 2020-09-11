package com.dataintuitive.viash.helpers

import scala.io.Source

object Bash {
  private def readUtils(s: String) = {
    val path = s"com/dataintuitive/viash/helpers/bashutils/$s.sh"
    Source.fromResource(path).getLines().mkString("\n")
  }

  lazy val ViashQuote: String = readUtils("ViashQuote")
  lazy val ViashExtractFlags: String = readUtils("ViashExtractFlags")
  lazy val ViashRemoveFlags: String = readUtils("ViashRemoveFlags")
  lazy val ViashAbsolutePath: String = readUtils("ViashAbsolutePath")
  lazy val ViashAutodetectMount: String = readUtils("ViashAutodetectMount")
  lazy val ViashSourceDir: String = readUtils("ViashSourceDir")

  def save(saveVariable: String, args: Seq[String]): String = {
    saveVariable + "=\"$" + saveVariable + " " + args.mkString(" ") + "\""
  }

  // generate strings in the form of:
  // SAVEVARIABLE="$SAVEVARIABLE $(Quote arg1) $(Quote arg2)"
  def quoteSave(saveVariable: String, args: Seq[String]): String = {
    saveVariable + "=\"$" + saveVariable +
      args.map(" $(ViashQuote \"" + _ + "\")").mkString +
      "\""
  }

  def argStore(name: String, plainName: String, store: String, argsConsumed: Int, storeUnparsed: Option[String]): String = {
    val passStr =
      if (storeUnparsed.isDefined) {
        "\n            " + quoteSave(storeUnparsed.get, (1 to argsConsumed).map("$" + _))
      } else {
        ""
      }
    s"""         $name)
      |            $plainName=$store$passStr
      |            shift $argsConsumed
      |            ;;""".stripMargin
  }
  def argStoreSed(name: String, plainName: String, storeUnparsed: Option[String]): String = {
    argStore(name + "=*", plainName, "$(ViashRemoveFlags \"$1\")", 1, storeUnparsed)
  }

}
