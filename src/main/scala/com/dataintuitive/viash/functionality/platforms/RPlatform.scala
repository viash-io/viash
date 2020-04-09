package com.dataintuitive.viash.functionality.platforms

import com.dataintuitive.viash.functionality._

case object RPlatform extends Platform {
  val `type` = "R"
  val commentStr = "#"
  
  def command(script: String) = {
    "Rscript " + script
  }
  
  private def removeNewlines(s: String) = 
      s.filter(_ >= ' ') // remove all control characters
      
  def generateArgparse(functionality: Functionality): String = {
    val params = functionality.arguments.filter(d => d.direction == Input || d.isInstanceOf[FileObject])
    
    // gather params for optlist
    val paramOptions = params.map(param => {
      val start = (
          param.name ::
          param.alternatives.getOrElse(Nil)
        ).mkString("\"", "\", \"", "\"")
      val helpStr = param.description.map(", help = \"" + removeNewlines(_) + "\"").getOrElse("")
      val requiredStr = 
        if (param.otype == "" || param.required.isEmpty) {
          ""
        } else {
          ", required = " + { if (param.required.get) "TRUE" else "FALSE" }
        }

      param match {
        case o: BooleanObject => {
          val storeStr = o.flagValue
            .map(fv => "action=\"store_" + { if (fv) "true" else "false" } + "\"")
            .getOrElse("type = \"logical\"")
          val defaultStr = o.default.map(d => ", default = " + { if (d) "TRUE" else "FALSE" }).getOrElse("")
          s"""parser$$add_argument($start, $storeStr$defaultStr$requiredStr$helpStr)\n"""
        }
        case o: DoubleObject => {
          val defaultStr = o.default.map(d => ", default = " + d).getOrElse("")
          s"""parser$$add_argument($start, type = "double"$defaultStr$requiredStr$helpStr)\n"""
        }
        case o: IntegerObject => {
          val defaultStr = o.default.map(d => ", default = " + d).getOrElse("")
          s"""parser$$add_argument($start, type = "integer"$defaultStr$requiredStr$helpStr)\n"""
        }
        case o: StringObject => {
          val defaultStr = o.default.map(d => ", default = \"" + d + "\"").getOrElse("")
          s"""parser$$add_argument($start, type = "character"$defaultStr$requiredStr$helpStr)\n"""
        }
        case o: FileObject => {
          val defaultStr = o.default.map(d => ", default = \"" + d + "\"").getOrElse("")
          s"""parser$$add_argument($start, type = "character", metavar="FILE"$defaultStr$requiredStr$helpStr)\n"""
        }
      }
    })

    // gather description
    val descrStr = functionality.description.map(",\n  description = \"" + removeNewlines(_) + "\"").getOrElse("")

    // construct file exist checks
    val reqFiles = params
        .filter(_.isInstanceOf[FileObject])
        .map(_.asInstanceOf[FileObject])
        .filter(_.mustExist.getOrElse(false))
    val reqFileStr = 
      if (reqFiles.isEmpty) {
        ""
      } else {
        s"""for (required_file in c("${reqFiles.map(_.plainName).mkString("\", \"")}")) {
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
            s"""if (!par[["${par.plainName}"]] %in% c("${par.values.get.mkString("\", \"")}")) {
              |  stop('"${par.plainName}" must be one of "${par.values.get.mkString("\", \"")}".')
              |}""".stripMargin
        }.mkString("")
      }

    s"""library(argparse, warn.conflicts = FALSE)
      |
      |parser <- ArgumentParser(
      |  usage = ""$descrStr
      |)
      |${paramOptions.mkString("")}
      |par <- parser$$parse_args()
      |
      |# checking inputs
      |$reqFileStr
      |$allinParCheck""".stripMargin
  }
}  
