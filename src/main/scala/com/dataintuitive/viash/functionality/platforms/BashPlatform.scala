package com.dataintuitive.viash.functionality.platforms

import com.dataintuitive.viash.functionality._

case object BashPlatform extends Platform {
  val `type` = "bash"
  val commentStr = "#"
  
  def command(script: String) = {
    "bash " + script
  }
  
  private def removeNewlines(s: String) = 
      s.filter(_ >= ' ') // remove all control characters
      
  def generateArgparse(functionality: Functionality): String = {
    // check whether functionality contains positional arguments
    functionality.arguments.foreach(arg =>
      require(arg.otype != "", message = "Positional arguments are not yet supported in bash.")
    )
    functionality.arguments.foreach{
      case o: BooleanObject =>
        require(o.flagValue.isEmpty, message = "boolean with flagvalues are not yet supported in bash.")
      case _ => {}
    }
    
    val params = functionality.arguments.filter(d => d.direction == Input || d.isInstanceOf[FileObject])
    
    // gather params for optlist
    val caseStrs = params.map(param => {
      val part1 = s"""        ${param.name})
        |        PAR[${param.plainName}]="$$2"
        |        shift 2 # past argument and value
        |        ;;
        |""".stripMargin
      val part2 = param.otype match {
          case "--" => 
            List(s"""        ${param.name}=*)
              |        PAR[${param.plainName}]=`echo $$1 | sed 's/^${param.name}=//'`
              |        shift 2 # past argument and value
              |        ;;
              |""".stripMargin)
          case "" => Nil
        }
      val moreParts = param.alternatives.getOrElse(Nil).map(alt => {
        val pattern = "^(-*)(.*)$".r
        val pattern(otype, plainName) = alt
        s"""        ${alt})
          |        PAR[${param.plainName}]="$$2"
          |        shift 2 # past argument and value
          |        ;;
          |""".stripMargin
      })
    
      (part1 :: part2 ::: moreParts).mkString
    })
    
    // TODO: allow description to have newlines
    // TODO: generate options documentation
    
    // construct required arg checks
    val defaultsStr = ""
    
    s"""Help()
      |{
      |   # Display Help
      |   echo "Usage:" # ... TODO fillin
      |   echo "${functionality.description.map(removeNewlines).getOrElse("")}"
      |   echo
      |   echo "Options:"
      |   echo "    -i INPUT, --input=INPUT"
      |   echo "        The path to a table to be filtered."
      |   echo
      |   echo "    -h, --help"
      |   echo "        Show this help message and exit"
      |   echo
      |}
      |declare -A PAR
      |POSITIONAL=()
      |
      |while [[ $$# -gt 0 ]]; do
      |    case "$$1" in
      |        -h)
      |        Help
      |        exit;;
      |        --help)
      |        Help
      |        exit;;
      |${caseStrs.mkString}        *)    # unknown option
      |        POSITIONAL+=("$$1") # save it in an array for later
      |        shift # past argument
      |        ;;
      |    esac
      |done
      |
      |set -- "$${POSITIONAL[@]}" # restore positional parameters
      |
      |# provide defaults
      |$defaultsStr""".stripMargin
  }
}  
