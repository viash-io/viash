/*
 * Copyright (C) 2020  Data Intuitive
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.viash.wrapper

import io.viash.functionality._
import io.viash.functionality.resources._
import io.viash.functionality.arguments._
import io.viash.helpers.{Bash, Format, Helper}

object BashWrapper {
  val metaFields: List[(String, String)] = {
    List(
      ("VIASH_META_FUNCTIONALITY_NAME", "functionality_name"),
      ("VIASH_META_RESOURCES_DIR", "resources_dir"),
      ("VIASH_META_EXECUTABLE", "executable"),
      ("VIASH_TEMP", "temp_dir")
    )
  }
  val var_resources_dir = "VIASH_META_RESOURCES_DIR"
  val var_executable = "VIASH_META_EXECUTABLE"

  def store(env: String, value: String, multiple_sep: Option[Char]): Array[String] = {
    if (multiple_sep.isDefined) {
      s"""if [ -z "$$$env" ]; then
         |  $env=$value
         |else
         |  $env="$$$env${multiple_sep.get}"$value
         |fi""".stripMargin.split("\n")
    } else {
      Array(env + "=" + value)
    }
  }

  def argStore(
    name: String,
    plainName: String,
    store: String,
    argsConsumed: Int,
    multiple_sep: Option[Char] = None
  ): String = {
    argsConsumed match {
      case num if num > 1 =>
        s"""        $name)
           |            ${this.store(plainName, store, multiple_sep).mkString("\n            ")}
           |            [ $$# -lt $argsConsumed ] && ViashError Not enough arguments passed to $name. Use "--help" to get more information on the parameters. && exit 1
           |            shift $argsConsumed
           |            ;;""".stripMargin
      case _ =>
        s"""        $name)
           |            ${this.store(plainName, store, multiple_sep).mkString("\n            ")}
           |            shift $argsConsumed
           |            ;;""".stripMargin
    }
    
  }

  def argStoreSed(name: String, plainName: String, multiple_sep: Option[Char] = None): String = {
    argStore(name + "=*", plainName, "$(ViashRemoveFlags \"$1\")", 1, multiple_sep)
  }

  def spaceCode(str: String): String = {
    if (str != "") {
      "\n" + str + "\n"
    } else {
      str
    }
  }

  val var_verbosity = "VIASH_VERBOSITY"

  def wrapScript(
    executor: String,
    functionality: Functionality,
    mods: BashWrapperMods = BashWrapperMods(),
    debugPath: Option[String] = None
  ): String = {
    val mainResource = functionality.mainScript

    // check whether the wd needs to be set to the resources dir
    val cdToResources =
      if (functionality.set_wd_to_resources_dir) {
        s"""
          |cd "$$$var_resources_dir"""".stripMargin
      } else {
        ""
      }

    // DETERMINE HOW TO RUN THE CODE
    val executionCode = mainResource match {
      // if mainResource is empty (shouldn't be the case)
      case None => ""

      // if mainResource is simply an executable
      case Some(e: Executable) => " " + e.path.get + " $VIASH_EXECUTABLE_ARGS"

      // if we want to debug our code
      case Some(res) if debugPath.isDefined =>
        val code = res.readWithInjection(functionality).get
        val escapedCode = Bash.escapeMore(code)
        val deb = debugPath.get

        s"""
          |set -e
          |cat > "${debugPath.get}" << 'VIASHMAIN'
          |$escapedCode
          |VIASHMAIN
          |""".stripMargin

      // if mainResource is a script
      case Some(res) =>
        val code = res.readWithInjection(functionality).get
        val escapedCode = Bash.escapeMore(code)

        // check whether the script can be written to a temprorary location or
        // whether it needs to be a specific path
        val scriptSetup =
          s"""
            |tempscript=\\$$(mktemp "$$VIASH_TEMP/viash-run-${functionality.name}-XXXXXX")
            |function clean_up {
            |  rm "\\$$tempscript"
            |}
            |function interrupt {
            |  echo -e "\\nCTRL-C Pressed..."
            |  exit 1
            |}
            |trap clean_up EXIT
            |trap interrupt INT SIGINT""".stripMargin
        val scriptPath = "\\$tempscript"

        s"""
          |set -e$scriptSetup
          |cat > "$scriptPath" << 'VIASHMAIN'
          |$escapedCode
          |VIASHMAIN$cdToResources
          |${res.command(scriptPath)} &
          |wait "\\$$!"
          |""".stripMargin
    }

    // generate bash document
    val (heredocStart, heredocEnd) = mainResource match {
      case None => ("", "")
      case Some(_: Executable) => ("", "")
      case _ => ("cat << VIASHEOF | ", "\nVIASHEOF")
    }

    // generate script modifiers
    val params = functionality.allArguments
    val paramAndDummies = functionality.allArgumentsAndDummies

    val helpMods = generateHelp(functionality)
    val parMods = generateParsers(params, paramAndDummies)
    val execMods = mainResource match {
      case Some(_: Executable) => generateExecutableArgs(params)
      case _ => BashWrapperMods()
    }

    // combine
    val allMods = helpMods ++ parMods ++ mods ++ execMods

    // generate header
    val header = Helper.generateScriptHeader(functionality)
      .map(h => Bash.escapeMore(h))
      .mkString("# ", "\n# ", "")

    /* GENERATE BASH SCRIPT */
    s"""#!/usr/bin/env bash
       |
       |$header
       |
       |set -e
       |
       |if [ -z "$$VIASH_TEMP" ]; then
       |  VIASH_TEMP=$${VIASH_TEMP:-$$VIASH_TMPDIR}
       |  VIASH_TEMP=$${VIASH_TEMP:-$$VIASH_TEMPDIR}
       |  VIASH_TEMP=$${VIASH_TEMP:-$$VIASH_TMP}
       |  VIASH_TEMP=$${VIASH_TEMP:-$$TMPDIR}
       |  VIASH_TEMP=$${VIASH_TEMP:-$$TMP}
       |  VIASH_TEMP=$${VIASH_TEMP:-$$TEMPDIR}
       |  VIASH_TEMP=$${VIASH_TEMP:-$$TEMP}
       |  VIASH_TEMP=$${VIASH_TEMP:-/tmp}
       |fi
       |
       |# define helper functions
       |${Bash.ViashQuote}
       |${Bash.ViashRemoveFlags}
       |${Bash.ViashSourceDir}
       |${Bash.ViashLogging}
       |
       |# find source folder of this component
       |$var_resources_dir=`ViashSourceDir $${BASH_SOURCE[0]}`
       |
       |# backwards compatibility
       |VIASH_RESOURCES_DIR="$$$var_resources_dir"
       |
       |# define meta fields
       |VIASH_META_FUNCTIONALITY_NAME="${functionality.name}"
       |VIASH_META_EXECUTABLE="$$VIASH_META_RESOURCES_DIR/$$VIASH_META_FUNCTIONALITY_NAME"
       |
       |${spaceCode(allMods.preParse)}
       |# initialise array
       |VIASH_POSITIONAL_ARGS=''
       |VIASH_MODE='run'
       |
       |while [[ $$# -gt 0 ]]; do
       |    case "$$1" in
       |        -h|--help)
       |            ViashHelp
       |            exit
       |            ;;
       |        ---v|---verbose)
       |            let "$var_verbosity=$var_verbosity+1"
       |            shift 1
       |            ;;
       |        ---verbosity)
       |            $var_verbosity="$$2"
       |            shift 2
       |            ;;
       |        ---verbosity=*)
       |            $var_verbosity="$$(ViashRemoveFlags "$$1")"
       |            shift 1
       |            ;;
       |        --version)
       |            echo "${Helper.nameAndVersion(functionality)}"
       |            exit
       |            ;;
       |${allMods.parsers}
       |        *)  # positional arg or unknown option
       |            # since the positional args will be eval'd, can we always quote, instead of using ViashQuote
       |            VIASH_POSITIONAL_ARGS="$$VIASH_POSITIONAL_ARGS '$$1'"
       |            [[ $$1 == -* ]] && ViashWarning $$1 looks like a parameter but is not a defined parameter and will instead be treated as a positional argument. Use "--help" to get more information on the parameters.
       |            shift # past argument
       |            ;;
       |    esac
       |done
       |
       |# parse positional parameters
       |eval set -- $$VIASH_POSITIONAL_ARGS
       |${spaceCode(allMods.postParse)}
       |${spaceCode(allMods.preRun)}
       |$heredocStart$executor$executionCode$heredocEnd
       |${spaceCode(allMods.postRun)}""".stripMargin
  }


  private def generateHelp(functionality: Functionality) = {
    val help = Helper.generateHelp(functionality)
    val helpStr = help.map(h => Bash.escapeMore(h, quote = true)).mkString("  echo \"", "\"\n  echo \"", "\"")

    val preParse =
      s"""# ViashHelp: Display helpful explanation about this executable
      |function ViashHelp {
      |$helpStr
      |}""".stripMargin

    BashWrapperMods(preParse = preParse)
  }

  private def generateParsers(params: List[Argument[_]], paramsAndDummies: List[Argument[_]]) = {
    // gather parse code for params
    val wrapperParams = params.filterNot(_.flags == "")
    val parseStrs = wrapperParams.map {
      case bo: BooleanArgumentBase if bo.flagValue.isDefined =>
        val fv = bo.flagValue.get

        // params of the form --param
        val part1 = argStore(bo.name, bo.VIASH_PAR, fv.toString, 1)
        // Alternatives
        val moreParts = bo.alternatives.map(alt => {
          argStore(alt, bo.VIASH_PAR, fv.toString, 1)
        })

        (part1 :: moreParts).mkString("\n")
      case param =>
        val multisep = if (param.multiple) Some(param.multiple_sep) else None

        // params of the form --param ...
        val part1 = param.flags match {
          case "---" | "--" | "-" => argStore(param.name, param.VIASH_PAR, "\"$2\"", 2, multisep)
          case "" => Nil
        }
        // params of the form --param=..., except -param=... is not allowed
        val part2 = param.flags match {
          case "---" | "--" => List(argStoreSed(param.name, param.VIASH_PAR, multisep))
          case "-" | "" => Nil
        }
        // Alternatives
        val moreParts = param.alternatives.map(alt => {
          argStore(alt, param.VIASH_PAR, "\"$2\"", 2, multisep)
        })

        (part1 :: part2 ::: moreParts).mkString("\n")
    }.mkString("\n")

    // parse positionals
    val positionals = paramsAndDummies.filter(_.flags == "")
    val positionalStr = positionals.map { param =>
      if (param.multiple) {
        s"""while [[ $$# -gt 0 ]]; do
           |  ${store(param.VIASH_PAR, "\"$1\"", Some(param.multiple_sep)).mkString("\n  ")}
           |  shift 1
           |done""".stripMargin
      } else {
        s"""if [[ $$# -gt 0 ]]; then
           |  ${param.VIASH_PAR}="$$1"
           |  shift 1
           |fi"""
      }
    }.mkString("\n")

    // construct required checks
    val reqParams = paramsAndDummies.filter(_.required)
    val reqCheckStr =
      if (reqParams.isEmpty) {
        ""
      } else {
        "\n# check whether required parameters exist\n" +
          reqParams.map { param =>
            s"""if [ -z "$$${param.VIASH_PAR}" ]; then
               |  ViashError '${param.name}' is a required argument. Use "--help" to get more information on the parameters.
               |  exit 1
               |fi""".stripMargin
          }.mkString("\n")
      }

    // construct default values, e.g.
    // if [ -z "$VIASH_PAR_FOO" ]; then
    //   VIASH_PAR_FOO="defaultvalue"
    // fi
    val defaultsStrs = paramsAndDummies.flatMap { param =>
      // if boolean argument has a flagvalue, add the inverse of it as a default value
      val default = param match {
        case p if p.required => None
        case bo: BooleanArgumentBase if bo.flagValue.isDefined => bo.flagValue.map(!_)
        case p if p.default.nonEmpty => Some(p.default.map(_.toString).mkString(p.multiple_sep.toString))
        case p if p.default.isEmpty => None
      }

      default.map(default => {
        s"""if [ -z "$$${param.VIASH_PAR}" ]; then
           |  ${param.VIASH_PAR}="${Bash.escapeMore(default.toString, quote = true, newline = true)}"
           |fi""".stripMargin
      })
    }.mkString("\n")

    // construct required file checks
    val reqFiles = paramsAndDummies.flatMap {
      case f: FileArgument if f.must_exist => Some(f)
      case _ => None
    }
    val reqFilesStr =
      if (reqFiles.isEmpty) {
        ""
      } else {
        "\n# check whether required files exist\n" +
          reqFiles.map { param =>
            if (param.multiple) {
              s"""if [ ! -z "$$${param.VIASH_PAR}" ]; then
                 |  IFS=${param.multiple_sep}
                 |  set -f
                 |  for file in $$${param.VIASH_PAR}; do
                 |    unset IFS
                 |    if [ ! -e "$$file" ]; then
                 |      ViashError "File '$$file' does not exist."
                 |      exit 1
                 |    fi
                 |  done
                 |  set +f
                 |fi""".stripMargin
          } else {
            s"""if [ ! -z "$$${param.VIASH_PAR}" ] && [ ! -e "$$${param.VIASH_PAR}" ]; then
               |  ViashError "File '$$${param.VIASH_PAR}' does not exist."
               |  exit 1
               |fi""".stripMargin
            }
          }.mkString("\n")
      }

    // construct type checks
    def typeMinMaxCheck[T](param: Argument[T], regex: String, min: Option[T] = None, max: Option[T] = None) = {
      val typeWithArticle = param match {
        case i: IntegerArgument => "an " + param.`type`
        case _ => "a " + param.`type`
      }

      val typeCheck = s"""  if ! [[ "$$${param.VIASH_PAR}" =~ ${regex} ]]; then
                         |    ViashError '${param.name}' has to be ${typeWithArticle}. Use "--help" to get more information on the parameters.
                         |    exit 1
                         |  fi
                         |""".stripMargin
      val minCheck = min match {
        case _ if min.isDefined =>
          s"""  if command -v bc &> /dev/null; then
             |    if ! [[ `echo $$${param.VIASH_PAR} '>=' ${min.get} | bc` -eq 1 ]]; then
             |      ViashError '${param.name}' has be more than or equal to ${min.get}. Use "--help" to get more information on the parameters.
             |      exit 1
             |    fi
             |  else
             |    ViashWarning '${param.name}' specifies a minimum value but the value was not verified as \\'bc\\' is not present on the system.
             |  fi
             |""".stripMargin
        case _ => ""
      }
      val maxCheck = max match {
        case _ if max.isDefined =>
          s"""  if command -v bc &> /dev/null; then
             |    if ! [[ `echo $$${param.VIASH_PAR} '<=' ${max.get} | bc` -eq 1 ]]; then
             |      ViashError '${param.name}' has to be less than or equal to ${max.get}. Use "--help" to get more information on the parameters.
             |      exit 1
             |    fi
             |  else
             |    ViashWarning '${param.name}' specifies a maximum value but the value was not verified as \\'bc\\' is not present on the system.
             |  fi
             |""".stripMargin
        case _ => ""
      }

      param match {
        case param if param.multiple =>
          val checkStart = 
            s"""if [ -n "$$${param.VIASH_PAR}" ]; then
               |  IFS=${param.multiple_sep}
               |  set -f
               |  for val in $$${param.VIASH_PAR}; do
               |"""
          val checkEnd =
            s"""  done
               |  set +f
               |  unset IFS
               |fi
               |""".stripMargin
          // TODO add extra spaces for typeCheck, minCheck, maxCheck
          checkStart +
          (typeCheck + minCheck + maxCheck)
            .replaceAll(param.VIASH_PAR, "{val}") // use {val} in the 'for' loop
            .replaceAll("^", "  ").replaceAll("\\n  ", "\n    ") + // fix indentation in a brute force way
          checkEnd
        case _ =>
          val checkStart = s"""if [[ -n "$$${param.VIASH_PAR}" ]]; then
                              |""".stripMargin
          val checkEnd = """fi
                           |""".stripMargin
          checkStart + typeCheck + minCheck + maxCheck + checkEnd
      }
    }
    val typeMinMaxCheckStr =
      if (paramsAndDummies.isEmpty) {
        ""
      } else {
        "\n# check whether parameters values are of the right type\n" +
          paramsAndDummies.map { param =>
            param match {
              case io: IntegerArgument =>
                typeMinMaxCheck(io, "^[-+]?[0-9]+$", io.min, io.max)
              case dO: DoubleArgument =>
                typeMinMaxCheck(dO, "^[-+]?(\\.[0-9]+|[0-9]+(\\.[0-9]*)?)([eE][-+]?[0-9]+)?$", dO.min, dO.max)
              case bo: BooleanArgumentBase =>
                typeMinMaxCheck(bo, "^(true|True|TRUE|false|False|FALSE|yes|Yes|YES|no|No|NO)$")               
              case _ => ""
            }           
          }.mkString("\n")
      }


    def checkChoices[T](param: Argument[T], allowedChoices: List[T]) = {
      val allowedChoicesString = allowedChoices.mkString(param.multiple_sep.toString)

      param match {
        case _ if param.multiple =>
          s"""if [ ! -z "$$${param.VIASH_PAR}" ]; then
             |  ${param.VIASH_PAR}_CHOICES=("$allowedChoicesString")
             |  IFS=${param.multiple_sep}
             |  set -f
             |  for val in $$${param.VIASH_PAR}; do
             |    if ! [[ "${param.multiple_sep}$${${param.VIASH_PAR}_CHOICES[*]}${param.multiple_sep}" =~ "${param.multiple_sep}$${val}${param.multiple_sep}" ]]; then
             |      ViashError '${param.name}' specified value of \\'$${val}\\' is not in the list of allowed values. Use "--help" to get more information on the parameters.
             |      exit 1
             |    fi
             |  done
             |  set +f
             |  unset IFS
             |fi
             |""".stripMargin
        case _ =>
          s"""if [ ! -z "$$${param.VIASH_PAR}" ]; then
             |  ${param.VIASH_PAR}_CHOICES=("$allowedChoicesString")
             |  IFS=${param.multiple_sep}
             |  set -f
             |  if ! [[ "${param.multiple_sep}$${${param.VIASH_PAR}_CHOICES[*]}${param.multiple_sep}" =~ "${param.multiple_sep}$$${param.VIASH_PAR}${param.multiple_sep}" ]]; then
             |    ViashError '${param.name}' specified value of \\'$$${param.VIASH_PAR}\\' is not in the list of allowed values. Use "--help" to get more information on the parameters.
             |    exit 1
             |  fi
             |  set +f
             |  unset IFS
             |fi
             |""".stripMargin
      }
    }
    val choiceCheckStr =
      if (paramsAndDummies.isEmpty) {
        ""
      } else {
        "\n# check whether parameters values are of the right type\n" +
          paramsAndDummies.map { param =>
            param match {
              case io: IntegerArgument if io.choices != Nil =>
                checkChoices(io, io.choices)
              case so: StringArgument if so.choices != Nil =>
                checkChoices(so, so.choices)
              case _ => ""
            }           
          }.mkString("\n")
      }

    // return output
    BashWrapperMods(
      parsers = parseStrs,
      preRun = positionalStr + "\n" + reqCheckStr + "\n" + defaultsStrs + "\n" + reqFilesStr + "\n" + typeMinMaxCheckStr + "\n" + choiceCheckStr
    )
  }

  private def generateExecutableArgs(params: List[Argument[_]]) = {
    val inserts = params.map {
      case bo: BooleanArgumentBase if bo.flagValue.isDefined =>
        s"""
           |if [ "$$${bo.VIASH_PAR}" == "${bo.flagValue.get}" ]; then
           |  VIASH_EXECUTABLE_ARGS="$$VIASH_EXECUTABLE_ARGS ${bo.name}"
           |fi""".stripMargin
      case param =>
        val flag = if (param.flags == "") "" else " " + param.name

        if (param.multiple) {
          s"""
             |if [ ! -z "$$${param.VIASH_PAR}" ]; then
             |  IFS=${param.multiple_sep}
             |  set -f
             |  for val in $$${param.VIASH_PAR}; do
             |    unset IFS
             |    VIASH_EXECUTABLE_ARGS="$$VIASH_EXECUTABLE_ARGS$flag '$$val'"
             |  done
             |  set +f
             |fi""".stripMargin
        } else {
          s"""
             |if [ ! -z "$$${param.VIASH_PAR}" ]; then
             |  VIASH_EXECUTABLE_ARGS="$$VIASH_EXECUTABLE_ARGS$flag '$$${param.VIASH_PAR}'"
             |fi""".stripMargin
        }
    }

    BashWrapperMods(
      preRun = "\nVIASH_EXECUTABLE_ARGS=''" + inserts.mkString
    )
  }

}
