package com.dataintuitive.viash.functionality.platforms

import com.dataintuitive.viash.functionality._

case object BashPlatform extends Platform {
  val `type` = "bash"
  val commentStr = "#"

  def command(script: String) = {
    "bash " + script
  }

  private def removeNewlines(s: String) = {
      s.filter(_ >= ' ') // remove all control characters
  }

  def generateArgparse(functionality: Functionality): String = {
    // check whether functionality contains positional arguments
    /*functionality.arguments.foreach(arg =>
      require(arg.otype != "", message = "Positional arguments are not yet supported in bash.")
    )
    functionality.arguments.foreach{
      case o: BooleanObject =>
        require(o.flagValue.isEmpty, message = "boolean with flagvalues are not yet supported in bash.")
      case _ => {}
    }*/

    val params = functionality.arguments.filter(d => d.direction == Input || d.isInstanceOf[FileObject])

    // TODO: postparse checks:
    //  * does file exist?
    //  * is value in list of possible values?

    s"""${generateHelp(functionality, params)}
      |
      |${generateParser(functionality, params)}
      |
      |""".stripMargin
  }

  def generateHelp(functionality: Functionality, params: List[DataObject[_]]): String = {
    // TODO: allow description to have newlines

    // gather parse code for params
    val usageStrs = params.map(param => {
      val names = param.alternatives.getOrElse(Nil) ::: List(param.name)
      val exampleStrs =
        if (param.isInstanceOf[BooleanObject] && param.asInstanceOf[BooleanObject].flagValue.isDefined) {
          names
        } else {
          names.map(name => {
            name + {if (name.startsWith("--")) "=" else " "} + param.plainName.toUpperCase()
          })
        }
      val exampleStr = exampleStrs.mkString(", ")
      s"""   echo "    ${exampleStrs.mkString(", ")}"
        |   echo "        ${param.description.getOrElse("")}"
        |   echo""".stripMargin
    })

    s"""Help()
      |{
      |   # Display Help
      |   echo "Usage:" # ... TODO fillin
      |   echo "${functionality.description.map(removeNewlines).getOrElse("")}"
      |   echo
      |   echo "Options:"
      |${usageStrs.mkString("\n")}
      |}""".stripMargin
  }

  def generateParser(functionality: Functionality, params: List[DataObject[_]]): String = {
    val wrapperParams =
      params
        .filter(_.name.contains("---"))
    // gather parse code for params
    val parseStrs = wrapperParams.map(param => {
      val part1 = s"""        ${param.name.drop(1)})
        |            par_${param.plainName}="$$2"
        |            shift 2 # past argument and value
        |            ;;""".stripMargin
      val part2 = param.otype match {
          case "---" =>
            List(s"""        ${param.name.drop(1)}=*)
              |            par_${param.plainName}=`echo $$1 | sed 's/^${param.name}=//'`
              |            shift 2 # past argument and value
              |            ;;""".stripMargin)
          case "-" => Nil
          case "" => Nil
        }
      val moreParts = param.alternatives.getOrElse(Nil).map(alt => {
        val pattern = "^(-*)(.*)$".r
        val pattern(otype, plainName) = alt
        s"""        ${alt})
          |            par_${param.plainName}="$$2"
          |            shift 2 # past argument and value
          |            ;;""".stripMargin
      })

      (part1 :: part2 ::: moreParts).mkString("\n")
    })

    // construct required arg checks
    val defaultsStrs = params.flatMap(param => {
      param.default.map("par_" + param.plainName + "=\"" + _ + "\"")
    })

    s"""# initialise array
      |POSITIONAL=()
      |
      |# initialise defaults
      |${defaultsStrs.mkString("\n")}
      |
      |# parse arguments
      |while [[ $$# -gt 0 ]]; do
      |    case "$$1" in
      |        -h)
      |            Help
      |            exit;;
      |        --help)
      |            Help
      |            exit;;
      |${parseStrs.mkString("\n")}
      |        *)    # unknown option
      |            CMDARGS="$$CMDARGS $$1"
      |            POSITIONAL+=("$$1") # save it in an array for later
      |            shift # past argument
      |            ;;
      |    esac
      |done
      |
      |# restore positional parameters
      |set -- "$${POSITIONAL[@]}"""".stripMargin
  }
}
