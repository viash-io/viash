package com.dataintuitive.viash.functionality.resources

import com.dataintuitive.viash.functionality._
import com.dataintuitive.viash.functionality.dataobjects._
import com.dataintuitive.viash.helpers.BashHelper

case class BashScript(
  name: Option[String] = None,
  path: Option[String] = None,
  text: Option[String] = None,
  is_executable: Boolean = true
) extends Script {
  val `type` = "bash_script"
  val commentStr = "#"

  def command(script: String) = {
    "bash \"" + script + "\""
  }

  private def removeNewlines(s: String) = {
      s.filter(_ >= ' ') // remove all control characters
  }

  def generateArgparse(functionality: Functionality): String = {
    val params = functionality.arguments.filter(d => d.direction == Input || d.isInstanceOf[FileObject])

    // TODO: postparse checks:
    //  * does file exist?
    //  * is value in list of possible values?

    s"""${BashHelper.quoteFunction}
      |${BashHelper.removeFlagFunction}
      |
      |${generateHelp(functionality, params)}
      |
      |${generateParser(functionality, params)}
      |
      |resources_dir="$$RESOURCES_DIR"
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

    s"""function Help {
      |   # Display Help
      |   echo "Usage:" # ... TODO fillin
      |   echo "${functionality.description.map(removeNewlines).getOrElse("")}"
      |   echo
      |   echo "Options:"
      |${usageStrs.mkString("\n")}
      |}""".stripMargin
  }

  def generateParser(functionality: Functionality, params: List[DataObject[_]]): String = {
    // construct default values, e.g.
    // par_foo="defaultvalue"
    val defaultsStrs = params.flatMap(param => {
      // if boolean object has a flagvalue, add the inverse of it as a default value
      val default =
        if (param.isInstanceOf[BooleanObject] && param.asInstanceOf[BooleanObject].flagValue.isDefined) {
          param.asInstanceOf[BooleanObject].flagValue.map(!_)
        } else {
          param.default
        }

      default.map("par_" + param.plainName + "=\"" + _ + "\"")
    })

    // gather parse code for params
    val wrapperParams = params.filterNot(_.otype == "")
    val parseStrs = wrapperParams.map(param => {
      val passthroughVariable = if (param.passthrough) Some("PASSTHROUGH") else None
      val storeVariable = "par_" + param.plainName

      if (param.isInstanceOf[BooleanObject] && param.asInstanceOf[BooleanObject].flagValue.isDefined) {
        val bo = param.asInstanceOf[BooleanObject]
        val fv = bo.flagValue.get

        // params of the form --param ...
        val part1 = BashHelper.argStore(param.name, storeVariable, fv.toString(), 1, passthroughVariable)
        // Alternatives
        val moreParts = param.alternatives.getOrElse(Nil).map(alt => {
          BashHelper.argStore(alt, storeVariable, fv.toString(), 1, passthroughVariable)
        })

        (part1 :: moreParts).mkString("\n")
      } else {
        // params of the form --param ...
        val part1 = param.otype match {
          case "---" | "--" | "-" => BashHelper.argStore(param.name, storeVariable, "\"$2\"", 2, passthroughVariable)
          case "" => Nil
        }
        // params of the form --param=..., except -param=... is not allowed
        val part2 = param.otype match {
            case "---" | "--" => List(BashHelper.argStoreSed(param.name, storeVariable, passthroughVariable))
            case "-" | "" => Nil
          }
        // Alternatives
        val moreParts = param.alternatives.getOrElse(Nil).map(alt => {
          BashHelper.argStore(alt, storeVariable, "\"$2\"", 2, passthroughVariable)
        })

        (part1 :: part2 ::: moreParts).mkString("\n")
      }
    })

    // parse positionals
    val positionals = params.filter(_.otype == "")
    val positionalStr = positionals.zipWithIndex.map{ case (param, index) =>
      "par_" + param.plainName + "=\"$" + (index+1) + "\""
    }.mkString("\n")

    // construct required checks
    val reqParams = params.filter(p => p.required.getOrElse(false))
    val reqCheckStr =
      if (reqParams.isEmpty) {
        ""
      } else {
        "\n# check whether required parameters exist\n" +
          reqParams.map{ param =>
            s"""if [ -z "$$par_${param.plainName}" ]; then
              |  echo '${param.name}' is a required argument. Use "--help" to get more information on the parameters.
              |  exit 1
              |fi""".stripMargin
          }.mkString("\n")
      }

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
      |${"" /*DO NOT INCLUDE PASSTHROUGH FOR * FOR NOW            PASSTHROUGH="PASSTHROUGH '$$1'"*/}
      |            POSITIONAL+=("$$1") # save it in an array for later
      |            shift # past argument
      |            ;;
      |    esac
      |done
      |
      |# parse positional parameters
      |set -- "$${POSITIONAL[@]}"
      |$positionalStr
      |$reqCheckStr""".stripMargin
  }
}
