package com.dataintuitive.viash.functionality.resources

import com.dataintuitive.viash.functionality._
import com.dataintuitive.viash.functionality.dataobjects._

case class PythonScript(
  name: Option[String] = None,
  path: Option[String] = None,
  text: Option[String] = None,
  is_executable: Boolean = true
) extends Script {
  val `type` = "python_script"

  val commentStr = "#"

  def command(script: String) = {
    "python \"" + script + "\""
  }
  def commandSeq(script: String) = {
    Seq("python", script)
  }

  def generateArgparse(functionality: Functionality): String = {
    val params = functionality.arguments.filter(d => d.direction == Input || d.isInstanceOf[FileObject])

    // gather params for optlist
    val paramOptions = params.map(param => {
      val start = (
          param.name ::
          param.alternatives
        ).mkString("\"", "\", \"", "\"")
      val helpStr = param.description.map(", help = \"\"\"" + _ + "\"\"\"").getOrElse("")
      val requiredStr =
        if (param.otype == "") {
          ""
        } else {
          ", required = " + { if (param.required) "True" else "False" }
        }

      param match {
        case o: BooleanObject => {
          val storeStr = o.flagValue
            .map(fv => "action='store_" + { if (fv) "true" else "false" } + "'")
            .getOrElse("type = bool")
          val defaultStr = o.default.map(d => ", default = " + { if (d) "True" else "False" }).getOrElse("")
          s"""parser.add_argument($start, $storeStr$defaultStr$requiredStr$helpStr)\n"""
        }
        case o: DoubleObject => {
          val defaultStr = o.default.map(d => ", default = " + d).getOrElse("")
          s"""parser.add_argument($start, type = float$defaultStr$requiredStr$helpStr)\n"""
        }
        case o: IntegerObject => {
          val defaultStr = o.default.map(d => ", default = " + d).getOrElse("")
          s"""parser.add_argument($start, type = int$defaultStr$requiredStr$helpStr)\n"""
        }
        case o: StringObject => {
          val defaultStr = o.default.map(d => ", default = \"" + d + "\"").getOrElse("")
          s"""parser.add_argument($start, type = str$defaultStr$requiredStr$helpStr)\n"""
        }
        case o: FileObject => {
          val defaultStr = o.default.map(d => ", default = \"" + d + "\"").getOrElse("")
          s"""parser.add_argument($start, type = str$defaultStr$requiredStr$helpStr)\n"""
        }
      }
    })

    // gather description
    val descrStr = functionality.description.map(",\n  description = \"\"\"" + _ + "\"\"\"").getOrElse("")

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
      |par = vars(parser.parse_args())
      |
      |# checking inputs
      |$reqFileStr
      |$allinParCheck
      |
      |resources_dir = "$$RESOURCES_DIR"
      |""".stripMargin
  }

  def generatePlaceholder(functionality: Functionality): String = {
    val params = functionality.arguments.filter(d => d.direction == Input || d.isInstanceOf[FileObject])

    val par_set = params.map{ par =>
      val env_name = "VIASH_PAR_" + par.plainName.toUpperCase()

      val parse = par match {
        case o: BooleanObject if o.multiple =>
          s"""list(map(lambda x: (x.lower() == 'true'), '$$$env_name'.split('${o.multiple_sep}')))"""
        case o: IntegerObject if o.multiple =>
          s"""list(map(int, '$$$env_name'.split('${o.multiple_sep}')))"""
        case o: DoubleObject if o.multiple =>
          s"""list(map(float, '$$$env_name'.split('${o.multiple_sep}')))"""
        case o: FileObject if o.multiple =>
          s"""'$$$env_name'.split('${o.multiple_sep}')"""
        case o: StringObject if o.multiple =>
          s"""'$$$env_name'.split('${o.multiple_sep}')"""
        case o: BooleanObject => s"""'$$$env_name'.lower() == 'true'"""
        case o: IntegerObject => s"""int('$$$env_name')"""
        case o: DoubleObject => s"""float('$$$env_name')"""
        case o: FileObject => s"""'$$$env_name'"""
        case o: StringObject => s"""'$$$env_name'"""
      }

      s"""'${par.plainName}': $$VIASH_DOLLAR$$( if [ ! -z $${$env_name+x} ]; then echo "$parse"; else echo None; fi )"""
    }
    s"""par = {
      |  ${par_set.mkString(",\n  ")}
      |}
      |
      |resources_dir = '$$RESOURCES_DIR'
      |""".stripMargin
  }
}
