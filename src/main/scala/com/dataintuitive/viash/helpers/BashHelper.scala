package com.dataintuitive.viash.helpers

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
}