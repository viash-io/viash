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
import io.viash.helpers.Escaper

object BashWrapper {
  val metaArgs: List[Argument[_]] = {
    List(
      StringArgument("functionality_name", required = true, dest = "meta"),
      FileArgument("resources_dir", required = true, dest = "meta"),
      FileArgument("executable", required = true, dest = "meta"),
      FileArgument("temp_dir", required = true, dest = "meta"),
      IntegerArgument("cpus", required = false, dest = "meta"),
      LongArgument("memory_b", required = false, dest = "meta"),
      LongArgument("memory_kb", required = false, dest = "meta"),
      LongArgument("memory_mb", required = false, dest = "meta"),
      LongArgument("memory_gb", required = false, dest = "meta"),
      LongArgument("memory_tb", required = false, dest = "meta"),
      LongArgument("memory_pb", required = false, dest = "meta")
    )
  }

  def store(name: String, env: String, value: String, multiple_sep: Option[String]): Array[String] = {
    if (multiple_sep.isDefined) {
      s"""if [ -z "$$$env" ]; then
         |  $env=$value
         |else
         |  $env="$$$env${multiple_sep.get}"$value
         |fi""".stripMargin.split("\n")
    } else {
      Array(
        s"""[ -n "$$$env" ] && ViashError Bad arguments for option \\'$name\\': \\'$$$env\\' \\& \\'$$2\\' - you should provide exactly one argument for this option. && exit 1""",
        env + "=" + value
      )
    }
  }

  def argStore(
    name: String,
    plainName: String,
    store: String,
    argsConsumed: Int,
    multiple_sep: Option[String] = None
  ): String = {
    argsConsumed match {
      case num if num > 1 =>
        s"""        $name)
           |            ${this.store(name, plainName, store, multiple_sep).mkString("\n            ")}
           |            [ $$# -lt $argsConsumed ] && ViashError Not enough arguments passed to $name. Use "--help" to get more information on the parameters. && exit 1
           |            shift $argsConsumed
           |            ;;""".stripMargin
      case _ =>
        s"""        $name)
           |            ${this.store(name, plainName, store, multiple_sep).mkString("\n            ")}
           |            shift $argsConsumed
           |            ;;""".stripMargin
    }
    
  }

  def argStoreSed(name: String, plainName: String, multiple_sep: Option[String] = None): String = {
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
          |cd "$$VIASH_META_RESOURCES_DIR"""".stripMargin
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
        val escapedCode = Bash.escapeString(code, allowUnescape = true)
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
        val escapedCode = Bash.escapeString(code, allowUnescape = true)

        // check whether the script can be written to a temprorary location or
        // whether it needs to be a specific path
        val scriptSetup =
          s"""
            |tempscript=\\$$(mktemp "$$VIASH_META_TEMP_DIR/viash-run-${functionality.name}-XXXXXX")
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
    val params = functionality.getArgumentLikes(includeMeta = true)

    val helpMods = generateHelp(functionality)
    val computationalRequirementMods = generateComputationalRequirements(functionality)
    val parMods = generateParsers(params)
    val execMods = mainResource match {
      case Some(_: Executable) => generateExecutableArgs(params)
      case _ => BashWrapperMods()
    }

    // combine
    val allMods = helpMods ++ parMods ++ mods ++ execMods ++ computationalRequirementMods

    // generate header
    val header = Helper.generateScriptHeader(functionality)
      .map(h => Escaper(h, newline = true))
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
       |VIASH_META_RESOURCES_DIR=`ViashSourceDir $${BASH_SOURCE[0]}`
       |
       |# define meta fields
       |VIASH_META_FUNCTIONALITY_NAME="${functionality.name}"
       |VIASH_META_EXECUTABLE="$$VIASH_META_RESOURCES_DIR/$$VIASH_META_FUNCTIONALITY_NAME"
       |VIASH_META_TEMP_DIR="$$VIASH_TEMP"
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
       |ViashDebug "Running command: ${executor.replaceAll("^eval (.*)", "\\$(echo $1)")}"
       |$heredocStart$executor$executionCode$heredocEnd
       |${spaceCode(allMods.postRun)}""".stripMargin
  }


  private def generateHelp(functionality: Functionality) = {
    val help = Helper.generateHelp(functionality)
    val helpStr = help
      .map(h => Bash.escapeString(h, quote = true))
      .mkString("  echo \"", "\"\n  echo \"", "\"")

    val preParse =
      s"""# ViashHelp: Display helpful explanation about this executable
      |function ViashHelp {
      |$helpStr
      |}""".stripMargin

    BashWrapperMods(preParse = preParse)
  }

  private def generateParsers(params: List[Argument[_]]) = {
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
    val positionals = params.filter(_.flags == "")
    val positionalStr = positionals.map { param =>
      if (param.multiple) {
        s"""while [[ $$# -gt 0 ]]; do
           |  ${store("positionalArg", param.VIASH_PAR, "\"$1\"", Some(param.multiple_sep)).mkString("\n  ")}
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
    val reqParams = params.filter(_.required)
    val reqCheckStr =
      if (reqParams.isEmpty) {
        ""
      } else {
        "\n# check whether required parameters exist\n" +
          reqParams.map { param =>
            s"""if [ -z $${${param.VIASH_PAR}+x} ]; then
               |  ViashError '${param.name}' is a required argument. Use "--help" to get more information on the parameters.
               |  exit 1
               |fi""".stripMargin
          }.mkString("\n")
      }

    // construct default values, e.g.
    // if [ -z "$VIASH_PAR_FOO" ]; then
    //   VIASH_PAR_FOO="defaultvalue"
    // fi
    val defaultsStrs = params.flatMap { param =>
      // if boolean argument has a flagvalue, add the inverse of it as a default value
      val default = param match {
        case p if p.required => None
        case bo: BooleanArgumentBase if bo.flagValue.isDefined => bo.flagValue.map(!_)
        case p if p.default.nonEmpty => Some(p.default.map(_.toString).mkString(p.multiple_sep.toString))
        case p if p.default.isEmpty => None
      }

      default.map(default => {
        s"""if [ -z $${${param.VIASH_PAR}+x} ]; then
           |  ${param.VIASH_PAR}="${Bash.escapeString(default.toString, quote = true, newline = true, allowUnescape = true)}"
           |fi""".stripMargin
      })
    }.mkString("\n")

    // construct required file checks
    val reqFiles = params.flatMap {
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
                 |  IFS='${Bash.escapeString(param.multiple_sep, quote = true)}'
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

      val typeCheck = 
        s"""  if ! [[ "$$${param.VIASH_PAR}" =~ ${regex} ]]; then
          |    ViashError '${param.name}' has to be ${typeWithArticle}. Use "--help" to get more information on the parameters.
          |    exit 1
          |  fi
          |""".stripMargin

      def minCheckDouble(min: Double) = 
        s"""  if command -v bc &> /dev/null; then
          |    if ! [[ `echo $$${param.VIASH_PAR} '>=' $min | bc` -eq 1 ]]; then
          |      ViashError '${param.name}' has be more than or equal to $min. Use "--help" to get more information on the parameters.
          |      exit 1
          |    fi
          |  elif command -v awk &> /dev/null; then
          |    if ! [[ `awk -v n1=$$${param.VIASH_PAR} -v n2=$min 'BEGIN { print (n1 >= n2) ? "1" : "0" }'` -eq 1 ]]; then
          |      ViashError '${param.name}' has be more than or equal to $min. Use "--help" to get more information on the parameters.
          |      exit 1
          |    fi
          |  else
          |    ViashWarning '${param.name}' specifies a minimum value but the value was not verified as neither \\'bc\\' or \\`awk\\` are present on the system.
          |  fi
          |""".stripMargin
      def maxCheckDouble(max: Double) = 
        s"""  if command -v bc &> /dev/null; then
          |    if ! [[ `echo $$${param.VIASH_PAR} '<=' $max | bc` -eq 1 ]]; then
          |      ViashError '${param.name}' has to be less than or equal to $max. Use "--help" to get more information on the parameters.
          |      exit 1
          |    fi
          |  elif command -v awk &> /dev/null; then
          |    if ! [[ `awk -v n1=$$${param.VIASH_PAR} -v n2=$max 'BEGIN { print (n1 <= n2) ? "1" : "0" }'` -eq 1 ]]; then
          |      ViashError '${param.name}' has be less than or equal to $max. Use "--help" to get more information on the parameters.
          |      exit 1
          |    fi
          |  else
          |    ViashWarning '${param.name}' specifies a maximum value but the value was not verified as neither \\'bc\\' or \\'awk\\' are present on the system.
          |  fi
          |""".stripMargin
      def minCheckInt(min: Long) = 
        s"""  if [[ $$${param.VIASH_PAR} -lt $min ]]; then
          |    ViashError '${param.name}' has be more than or equal to $min. Use "--help" to get more information on the parameters.
          |    exit 1
          |  fi
          |""".stripMargin
      def maxCheckInt(max: Long) = 
        s"""  if [[ $$${param.VIASH_PAR} -gt $max ]]; then
          |    ViashError '${param.name}' has be less than or equal to $max. Use "--help" to get more information on the parameters.
          |    exit 1
          |  fi
          |""".stripMargin

      val minCheck = param match {
        case p: IntegerArgument if min.isDefined => minCheckInt(min.get)
        case p: LongArgument if min.isDefined => minCheckInt(min.get)
        case p: DoubleArgument if min.isDefined => minCheckDouble(min.get)
        case _ => ""
      }
      val maxCheck = param match {
        case p: IntegerArgument if max.isDefined => maxCheckInt(max.get)
        case p: LongArgument if max.isDefined => maxCheckInt(max.get)
        case p: DoubleArgument if max.isDefined => maxCheckDouble(max.get)
        case _ => ""
      }

      param match {
        case param if param.multiple =>
          val checkStart = 
            s"""if [ -n "$$${param.VIASH_PAR}" ]; then
               |  IFS='${Bash.escapeString(param.multiple_sep, quote = true)}'
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
      if (params.isEmpty) {
        ""
      } else {
        "\n# check whether parameters values are of the right type\n" +
          params.flatMap { param =>
            param match {
              case io: IntegerArgument =>
                Some(typeMinMaxCheck(io, "^[-+]?[0-9]+$", io.min, io.max))
              case io: LongArgument =>
                Some(typeMinMaxCheck(io, "^[-+]?[0-9]+$", io.min, io.max))
              case dO: DoubleArgument =>
                Some(typeMinMaxCheck(dO, "^[-+]?(\\.[0-9]+|[0-9]+(\\.[0-9]*)?)([eE][-+]?[0-9]+)?$", dO.min, dO.max))
              case bo: BooleanArgumentBase =>
                Some(typeMinMaxCheck(bo, "^(true|True|TRUE|false|False|FALSE|yes|Yes|YES|no|No|NO)$"))
              case _ => None
            }           
          }.mkString("\n")
      }


    def checkChoices[T](param: Argument[T], allowedChoices: List[T]) = {
      val allowedChoicesString = allowedChoices.mkString(param.multiple_sep.toString)

      param match {
        case _ if param.multiple =>
          s"""if [ ! -z "$$${param.VIASH_PAR}" ]; then
             |  ${param.VIASH_PAR}_CHOICES=("$allowedChoicesString")
             |  IFS='${Bash.escapeString(param.multiple_sep, quote = true)}'
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
             |  IFS='${Bash.escapeString(param.multiple_sep, quote = true)}'
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
      if (params.isEmpty) {
        ""
      } else {
        "\n# check whether parameters values are of the right type\n" +
          params.map { param =>
            param match {
              case io: IntegerArgument if io.choices != Nil =>
                checkChoices(io, io.choices)
              case io: LongArgument if io.choices != Nil =>
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


  private def generateComputationalRequirements(functionality: Functionality) = {
    val compArgs = List(
      ("---cpus", "VIASH_META_CPUS", functionality.requirements.cpus.map(_.toString)),
      ("---memory", "VIASH_META_MEMORY", functionality.requirements.memoryAsBytes.map(_.toString + "b"))
    )

    // gather parse code for params
    val parsers = 
      compArgs.flatMap{ case (flag, env, _) => 
        List(
          argStore(flag, env, "\"$2\"", 2),
          argStoreSed(flag, env)
        )
      }.map("\n" + _).mkString

    // construct default values, e.g.
    val defaultsStrs = compArgs.flatMap{ case (_, env, default) =>
      default.map(dflt => {
        s"""if [ -z $${$env+x} ]; then
           |  $env="$dflt"
           |fi""".stripMargin
      })
    }.mkString("\n")

    // calculators
    val memoryCalculations = 
      """# helper function for parsing memory strings
      |function ViashMemoryAsBytes {
      |  local memory=`echo "$1" | tr '[:upper:]' '[:lower:]' | tr -d '[:space:]'`
      |  local memory_regex='^([0-9]+)([kmgtp]b?|b)$'
      |  if [[ $memory =~ $memory_regex ]]; then
      |    local number=${memory/[^0-9]*/}
      |    local symbol=${memory/*[0-9]/}
      |    
      |    case $symbol in
      |      b)      memory_b=$number ;;
      |      kb|k)   memory_b=$(( $number * 1024 )) ;;
      |      mb|m)   memory_b=$(( $number * 1024 * 1024 )) ;;
      |      gb|g)   memory_b=$(( $number * 1024 * 1024 * 1024 )) ;;
      |      tb|t)   memory_b=$(( $number * 1024 * 1024 * 1024 * 1024 )) ;;
      |      pb|p)   memory_b=$(( $number * 1024 * 1024 * 1024 * 1024 * 1024 )) ;;
      |    esac
      |    echo "$memory_b"
      |  fi
      |}
      |# compute memory in different units
      |if [ ! -z ${VIASH_META_MEMORY+x} ]; then
      |  VIASH_META_MEMORY_B=`ViashMemoryAsBytes $VIASH_META_MEMORY`
      |  # do not define other variables if memory_b is an empty string
      |  if [ ! -z "$VIASH_META_MEMORY_B" ]; then
      |    VIASH_META_MEMORY_KB=$(( ($VIASH_META_MEMORY_B+1023) / 1024 ))
      |    VIASH_META_MEMORY_MB=$(( ($VIASH_META_MEMORY_KB+1023) / 1024 ))
      |    VIASH_META_MEMORY_GB=$(( ($VIASH_META_MEMORY_MB+1023) / 1024 ))
      |    VIASH_META_MEMORY_TB=$(( ($VIASH_META_MEMORY_GB+1023) / 1024 ))
      |    VIASH_META_MEMORY_PB=$(( ($VIASH_META_MEMORY_TB+1023) / 1024 ))
      |  else
      |    # unset memory if string is empty
      |    unset $VIASH_META_MEMORY_B
      |  fi
      |fi
      |# unset nproc if string is empty
      |if [ -z "$VIASH_META_CPUS" ]; then
      |  unset $VIASH_META_CPUS
      |fi""".stripMargin

    // return output
    BashWrapperMods(
      parsers = parsers,
      postParse = "\n" + defaultsStrs + "\n" + memoryCalculations
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
             |  IFS='${Bash.escapeString(param.multiple_sep, quote = true)}'
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
