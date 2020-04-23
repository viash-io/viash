package com.dataintuitive.viash.helpers

import com.dataintuitive.viash.functionality.DataObject

object BashHelper {
  val quoteFunction = {
    """function Quote {
      |  echo $1 | sed "s/'/'\"'\"'/g" | sed "s/^[^-].*/'&'/"
      |}""".stripMargin
  }

  // generate strings in the form of:
  // SAVEVARIABLE="$SAVEVARIABLE `Quote $arg1` `Quote $arg2`"
  def quoteSave(saveVariable: String, args: Seq[String]) = {
    saveVariable + "=\"$" + saveVariable +
      args.map(" `Quote \"" + _ + "\"`").mkString +
      "\""
  }

  def quoteSaves(saveVariable: String, args: String*) = {
    quoteSave(saveVariable, args)
  }

  def escape(str: String) = {
    str.replaceAll("([\\$`])", "\\\\$1")
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
    argStore(name + "=*", plainName, "`echo $1 | sed 's/^" + name + "=//'`", 1, storeUnparsed)
  }
}