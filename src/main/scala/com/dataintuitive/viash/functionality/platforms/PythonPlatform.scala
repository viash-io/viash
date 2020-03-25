package com.dataintuitive.viash.functionality.platforms

import com.dataintuitive.viash.functionality._

case object PythonPlatform extends Platform {
  val `type` = "Python"
  val commentStr = "#"
  
  def command(script: String) = {
    "python " + script
  }
  
  def generateArgparse(functionality: Functionality): String = {
    val params = functionality.inputs ::: functionality.outputs.filter(_.isInstanceOf[FileObject])
    
    // gather params for optlist
    val paramOptions = params.map {
      case o: BooleanObject => {
        val helpStr = o.description.map(", help = \"" + _ + "\"").getOrElse("")
        val defaultStr = o.default.map(d => ", default = " + { if (d) "True" else "False" }).getOrElse("")
        val requiredStr = o.required.map(r => ", required = " + { if (r) "True" else "False "}).getOrElse("")
        s"""parser.add_argument("--${o.name}", type = bool$defaultStr$requiredStr$helpStr)\n"""
      }
      case o: DoubleObject => {
        val helpStr = o.description.map(", help = \"" + _ + "\"").getOrElse("")
        val defaultStr = o.default.map(d => ", default = " + d).getOrElse("")
        val requiredStr = o.required.map(r => ", required = " + { if (r) "True" else "False "}).getOrElse("")
        s"""parser.add_argument("--${o.name}", type = float$defaultStr$requiredStr$helpStr)\n"""
      }
      case o: IntegerObject => {
        val helpStr = o.description.map(", help = \"" + _ + "\"").getOrElse("")
        val defaultStr = o.default.map(d => ", default = " + d).getOrElse("")
        val requiredStr = o.required.map(r => ", required = " + { if (r) "True" else "False "}).getOrElse("")
        s"""parser.add_argument("--${o.name}", type = int$defaultStr$requiredStr$helpStr)\n"""
      }
      case o: StringObject => {
        val helpStr = o.description.map(", help = \"" + _ + "\"").getOrElse("")
        val defaultStr = o.default.map(d => ", default = \"" + d + "\"").getOrElse("")
        val requiredStr = o.required.map(r => ", required = " + { if (r) "True" else "False "}).getOrElse("")
        s"""parser.add_argument("--${o.name}", type = str$defaultStr$requiredStr$helpStr)\n"""
      }
      case o: FileObject => {
        val helpStr = o.description.map(", help = \"" + _ + "\"").getOrElse("")
        val defaultStr = o.default.map(d => ", default = \"" + d + "\"").getOrElse("")
        val requiredStr = o.required.map(r => ", required = " + { if (r) "True" else "False "}).getOrElse("")
        s"""parser.add_argument("--${o.name}", type = str, metavar="FILE"$defaultStr$requiredStr$helpStr)\n"""
      }
    }
    
    // gather description 
    val descrStr = functionality.description.map(",\n  description = \"" + _ + "\"").getOrElse("")
    
    // TODO: implement these checks
//    // construct file exist checks
//    val reqFiles = params
//        .filter(_.isInstanceOf[FileObject])
//        .map(_.asInstanceOf[FileObject])
//        .filter(_.mustExist.getOrElse(false))
//    val reqFileStr = 
//      if (reqFiles.isEmpty) {
//        ""
//      } else {
//        s"""for (required_file in c("${reqFiles.map(_.name).mkString("\", \"")}")) {
//          |  if (!file.exists(par[[required_file]])) {
//          |    stop('file "', required_file, '" must exist.')
//          |  }
//          |}""".stripMargin
//      }
//    
//    // construct value all in set checks
//    val allinPars = params
//        .filter(_.isInstanceOf[StringObject])
//        .map(_.asInstanceOf[StringObject])
//        .filter(_.values.isDefined)
//    val allinParCheck = 
//      if (allinPars.isEmpty) {
//        ""
//      } else {
//        allinPars.map{
//          par =>
//            s"""if (!par[[${par.name}]] %in% c("${par.values.get.mkString("\", \"")}")) {
//              |  stop('"${par.name}" must be one of "${par.values.get.mkString("\", \"")}".')
//              |}""".stripMargin
//        }.mkString("")
//      }
    
    val reqFileStr = ""
    val allinParCheck = ""
    
    s"""import argparse
      |
      |parser = argparse.ArgumentParser(
      |  usage = ""$descrStr
      |)
      |${paramOptions.mkString("")}
      |par = parser.parse_args()
      |
      |# checking inputs
      |$reqFileStr
      |$allinParCheck""".stripMargin
  }
}
