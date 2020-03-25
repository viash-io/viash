package com.dataintuitive.viash.functionality.platforms

import com.dataintuitive.viash.functionality._

case object RPlatform extends Platform {
  val `type` = "R"
  val commentStr = "#"
  
  def command(script: String) = {
    "Rscript " + script
  }
  
  def generateArgparse(functionality: Functionality): String = {
    val params = functionality.inputs ::: functionality.outputs.filter(_.isInstanceOf[FileObject])
    
    // gather params for optlist
    val paramOptions = params.map {
      case o: BooleanObject => {
        val helpStr = o.description.map(", help = \"" + _ + "\"").getOrElse("")
        val defaultStr = o.default.map(d => ", default = " + { if (d) "TRUE" else "FALSE" }).getOrElse("")
        s"""make_option("--${o.name}", type = "logical"$helpStr$defaultStr)"""
      }
      case o: DoubleObject => {
        val helpStr = o.description.map(", help = \"" + _ + "\"").getOrElse("")
        val defaultStr = o.default.map(d => ", default = " + d).getOrElse("")
        s"""make_option("--${o.name}", type = "double"$helpStr$defaultStr)"""
      }
      case o: IntegerObject => {
        val helpStr = o.description.map(", help = \"" + _ + "\"").getOrElse("")
        val defaultStr = o.default.map(d => ", default = " + d).getOrElse("")
        s"""make_option("--${o.name}", type = "integer"$helpStr$defaultStr)"""
      }
      case o: StringObject => {
        val helpStr = o.description.map(", help = \"" + _ + "\"").getOrElse("")
        val defaultStr = o.default.map(d => ", default = \"" + d + "\"").getOrElse("")
        s"""make_option("--${o.name}", type = "character"$helpStr$defaultStr)"""
      }
      case o: FileObject => {
        val helpStr = o.description.map(", help = \"" + _ + "\"").getOrElse("")
        val defaultStr = o.default.map(d => ", default = \"" + d + "\"").getOrElse("")
        s"""make_option("--${o.name}", type = "character"$helpStr$defaultStr)"""
      }
    }
    
    // gather description 
    val descrStr = functionality.description.map("\ndescription = \"" + _ + "\",").getOrElse("")
    
    // construct required arg checks
    val reqParams = params.filter(_.required.getOrElse(false))
    val reqParamStr = 
      if (reqParams.isEmpty) {
        ""
      } else {
        s"""for (required_arg in c("${reqParams.map(_.name).mkString("\", \"")}")) {
          |  if (is.null(par[[required_arg]])) {
          |    stop('"--', required_arg, '" is a required argument. Use "--help" to get more information on the parameters.')
          |  }
          |}""".stripMargin
      }
    
    // construct file exist checks
    val reqFiles = params
        .filter(_.isInstanceOf[FileObject])
        .map(_.asInstanceOf[FileObject])
        .filter(_.mustExist.getOrElse(false))
    val reqFileStr = 
      if (reqFiles.isEmpty) {
        ""
      } else {
        s"""for (required_file in c("${reqFiles.map(_.name).mkString("\", \"")}")) {
          |  if (!file.exists(par[[required_file]])) {
          |    stop('file "', required_file, '" must exist.')
          |  }
          |}""".stripMargin
      }
    
    // construct value all in set checks
    val allinPars = params
        .filter(_.isInstanceOf[StringObject])
        .map(_.asInstanceOf[StringObject])
        .filter(_.values.isDefined)
    val allinParCheck = 
      if (allinPars.isEmpty) {
        ""
      } else {
        allinPars.map{
          par =>
            s"""if (!par[[${par.name}]] %in% c("${par.values.get.mkString("\", \"")}")) {
              |  stop('"${par.name}" must be one of "${par.values.get.mkString("\", \"")}".')
              |}""".stripMargin
        }.mkString("")
      }
    
    s"""library(optparse, warn.conflicts = FALSE)
      |
      |optlist <- list(
      |${paramOptions.mkString("  ", ",\n  ", "")}
      |)
      |
      |parser <- OptionParser(
      |  usage = "",$descrStr
      |  option_list = optlist
      |)
      |par <- parse_args(parser, args = commandArgs(trailingOnly = TRUE))
      |
      |# checking inputs
      |$reqParamStr
      |$reqFileStr
      |$allinParCheck""".stripMargin
  }
}