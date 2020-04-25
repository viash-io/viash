package com.dataintuitive.viash.functionality.platforms

import com.dataintuitive.viash.functionality._

case object RPlatform extends Platform {
  val `type` = "R"
  val commentStr = "#"

  def command(script: String) = {
    "Rscript " + script
  }

  private def removeNewlines(s: String) = {
    s.replaceAll("\n", "\\\\n")
  }

  def generateArgparse(functionality: Functionality): String = {
    // check whether functionality contains positional arguments
    val params = functionality.arguments.filter(d => d.direction == Input || d.isInstanceOf[FileObject])

    s"""library(optparse, warn.conflicts = FALSE)
      |
      |# construct parameter list
      |${makeOptList(functionality, params)}
      |
      |# construct parser
      |${makeParser(functionality, params)}
      |
      |${makeRequiredArgCheck(functionality, params)}
      |${makeRequiredFileCheck(functionality, params)}
      |${makeSubsetCheck(functionality, params)}
      |
      |resources_dir <- "$$VIASHDIR"
      |""".stripMargin
  }

  def makeOptList(functionality: Functionality, params: List[DataObject[_]]): String = {
    // gather params for optlist
    val paramOptions = params.filter(_.otype != "").map(param => {
      val start = (
          { if (param.otype == "-") List("-" + param.name) else Nil } ::: // R optparse does not support short flag only
          param.name ::
          param.alternatives.getOrElse(Nil)
        ).mkString("c(\"", "\", \"", "\")")
      val defStr = param.default.map(s => " [default %default]").getOrElse("")
      val helpStr = param.description.map(", help = \"" + removeNewlines(_) + defStr + "\"").getOrElse("")

      val typeStr = param match {
        case o: BooleanObject => "logical"
        case o: DoubleObject => "double"
        case o: IntegerObject => "integer"
        case o: StringObject => "character"
        case o: FileObject => "character"
      }

      val storeStr = param match {
        case o: BooleanObject if o.flagValue.isDefined =>
          ", action = \"store_" + { if (o.flagValue.get) "true" else "false" } + "\""
        case _ => ""
      }

      val defaultVal = param match {
        case o: BooleanObject =>
          o.flagValue.map(fv => Some(!fv)).getOrElse(o.default).map{ if (_) "TRUE" else "FALSE"}
        case o @ (_: DoubleObject | _:IntegerObject) =>
          o.default.map(_.toString())
        case o @ (_: StringObject | _: FileObject) =>
          o.default.map(d => "\"" + d + "\"")
      }

      val defaultStr = defaultVal.map(d => ", default = " + d).getOrElse("")

      s"""make_option($start, type = "$typeStr"$defaultStr$storeStr$helpStr)"""
    })

    s"""optlist <- list(
      |${paramOptions.mkString("  ", ",\n  ", "")}
      |)""".stripMargin
  }

  def makeParser(functionality: Functionality, params: List[DataObject[_]]): String = {
    val positionalParams = params.filter(_.otype == "")

    // gather description
    val usageStr = functionality.name + positionalParams.map(_.plainName.toUpperCase()).mkString(" ", " ", "") + " [OPTIONS]"

    val positionalParamStr =
      if (positionalParams.nonEmpty) {
        "\nPositional arguments:\n" +
        positionalParams.map{p =>
          s"\t${p.plainName.toUpperCase()}\n\t\t${p.description.getOrElse("")}"
        }.mkString
      } else {
        ""
      }
    val descrStr = removeNewlines(functionality.description.getOrElse("") + positionalParamStr)

    val assigner = positionalParams.zipWithIndex.map{ case (p, i) =>
      s"""\npar[["${p.plainName}"]] <- parsed[["args"]][[${i+1}]]"""
    }.mkString

    s"""parser <- OptionParser(
      |  usage = "$usageStr",
      |  description = "$descrStr",
      |  option_list = optlist
      |)
      |parsed <- parse_args(
      |  positional_arguments = ${positionalParams.length},
      |  object = parser,
      |  args = commandArgs(trailingOnly = TRUE)
      |)
      |par <- parsed$$options$assigner"""
  }

  def makeRequiredArgCheck(functionality: Functionality, params: List[DataObject[_]]): String = {
    // construct required arg checks
    val reqParams = params.filter(_.required.getOrElse(false))
    if (reqParams.isEmpty) {
      ""
    } else {
      s"""# check whether required parameters exist
        |for (required_arg in c("${reqParams.map(_.plainName).mkString("\", \"")}")) {
        |  if (is.null(par[[required_arg]])) {
        |    stop('"', required_arg, '" is a required argument. Use "--help" to get more information on the parameters.')
        |  }
        |}""".stripMargin
    }
  }

  def makeRequiredFileCheck(functionality: Functionality, params: List[DataObject[_]]): String = {
    val reqFiles = params
        .filter(_.isInstanceOf[FileObject])
        .map(_.asInstanceOf[FileObject])
        .filter(_.mustExist.getOrElse(false))
    if (reqFiles.isEmpty) {
      ""
    } else {
      s"""# check whether required files exist
        |for (required_file in c("${reqFiles.map(_.plainName).mkString("\", \"")}")) {
        |  if (!file.exists(par[[required_file]])) {
        |    stop('file "', required_file, '" must exist.')
        |  }
        |}""".stripMargin
    }
  }

  def makeSubsetCheck(functionality: Functionality, params: List[DataObject[_]]): String = {
    val subsetPars = params
        .filter(_.isInstanceOf[StringObject])
        .map(_.asInstanceOf[StringObject])
        .filter(_.values.isDefined)
    if (subsetPars.isEmpty) {
      ""
    } else {
      "# check whether arguments contain expected values\n" +
      subsetPars.map{
        par =>
          s"""if (!par[["${par.plainName}"]] %in% c("${par.values.get.mkString("\", \"")}")) {
            |  stop('"${par.plainName}" must be one of "${par.values.get.mkString("\", \"")}".')
            |}""".stripMargin
      }.mkString
    }
  }
}
