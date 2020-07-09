package com.dataintuitive.viash.helpers

import com.dataintuitive.viash.functionality._
import com.dataintuitive.viash.functionality.resources._
import java.nio.file.Paths

object BashHelper {
  val quoteFunction = {
    """# ViashQuote: put quotes around non flag values
      |# $1     : unquoted string
      |# return : possibly quoted string
      |# examples:
      |#   ViashQuote --foo      # returns --foo
      |#   ViashQuote bar        # returns 'bar'
      |#   Viashquote --foo=bar  # returns --foo='bar'
      |function ViashQuote {
      |  if [[ $1 =~ ^-+[a-zA-Z0-9_\-]+=.+$ ]]; then
      |    echo $1 | sed "s#=\(.*\)#='\1'#"
      |  elif [[ $1 =~ ^-+[a-zA-Z0-9_\-]+$ ]]; then
      |    echo $1
      |  else
      |    echo "'$1'"
      |  fi
      |}""".stripMargin
  }
  val removeFlagFunction = {
    """# ViashRemoveFlags: Remove leading flag
      |# $1     : string with a possible leading flag
      |# return : string without possible leading flag
      |# examples:
      |#   ViashRemoveFlags --foo=bar  # returns bar
      |function ViashRemoveFlags {
      |  echo $1 | sed 's/^--*[a-zA-Z0-9_\-]*=//'
      |}""".stripMargin
  }
  val absolutePathFunction = {
    """# ViashAbsolutePath: generate absolute path from relative path
      |# borrowed from https://stackoverflow.com/a/21951256
      |# $1     : relative filename
      |# return : absolute path
      |# examples:
      |#   ViashAbsolutePath some_file.txt   # returns /path/to/some_file.txt
      |#   ViashAbsolutePath /foo/bar/..     # returns /foo
      |function ViashAbsolutePath {
      |  local thePath
      |  if [[ ! "$1" =~ ^/ ]]; then
      |    thePath="$PWD/$1"
      |  else
      |    thePath="$1"
      |  fi
      |  echo "$thePath" | (
      |    IFS=/
      |    read -a parr
      |    declare -a outp
      |    for i in "${parr[@]}"; do
      |      case "$i" in
      |      ''|.) continue ;;
      |      ..)
      |        len=${#outp[@]}
      |        if ((len==0)); then
      |          continue
      |        else
      |          unset outp[$((len-1))]
      |        fi
      |        ;;
      |      *)
      |        len=${#outp[@]}
      |        outp[$len]="$i"
      |      ;;
      |      esac
      |    done
      |    echo /"${outp[*]}"
      |  )
      |}""".stripMargin
  }
  val detectMountFunction = {
    """# ViashDetectMount: auto configuring docker mounts from parameters
      |# $1                  : The parameter name
      |# $2                  : The parameter value
      |# returns             : New parameter
      |# $VIASH_EXTRA_MOUNTS : Added another parameter to be passed to docker
      |# examples:
      |#   ViashDetectMount --foo /path/to/bar
      |#   -> sets VIASH_EXTRA_MOUNTS to "-v /path/to:/viash_automount/foo"
      |#   -> returns /viash_automount/foo/bar
      |function ViashDetectMount {
      |  local PARNAME=`echo $1 | sed 's#^-*##'`
      |  local ABSPATH=`ViashAbsolutePath $2`
      |  local NEWMOUNT
      |  local NEWNAME
      |  if [ -d $ABSPATH ]; then
      |    NEWMOUNT=$ABSPATH
      |    NEWNAME=""
      |  else
      |    NEWMOUNT=`dirname $ABSPATH`
      |    NEWNAME=`basename $ABSPATH`
      |  fi
      |  VIASH_EXTRA_MOUNTS="-v \"$NEWMOUNT:/viash_automount/$PARNAME\" $VIASH_EXTRA_MOUNTS"
      |  echo "\"/viash_automount/$PARNAME/$NEWNAME\""
      |}""".stripMargin
    }

  // generate strings in the form of:
  // SAVEVARIABLE="$SAVEVARIABLE `Quote $arg1` `Quote $arg2`"
  def quoteSave(saveVariable: String, args: Seq[String]) = {
    saveVariable + "=\"$" + saveVariable +
      args.map(" `ViashQuote \"" + _ + "\"`").mkString +
      "\""
  }

  def quoteSaves(saveVariable: String, args: String*) = {
    quoteSave(saveVariable, args)
  }

  def escape(str: String) = {
    str.replaceAll("([\\\\$`])", "\\\\$1")
  }

  def argStore(name: String, plainName: String, store: String, argsConsumed: Int, storeUnparsed: Option[String]) = {
    val passStr =
      if (storeUnparsed.isDefined) {
        "\n            " + quoteSave(storeUnparsed.get, (1 to argsConsumed).map("$" + _))
      } else {
        ""
      }
    s"""         $name)
      |            ${plainName}=$store$passStr
      |            shift $argsConsumed
      |            ;;""".stripMargin
  }
  def argStoreSed(name: String, plainName: String, storeUnparsed: Option[String]) = {
    argStore(name + "=*", plainName, "`ViashRemoveFlags \"$1\"`", 1, storeUnparsed)
  }

  def wrapScript(
      executor: String,
      functionality: Functionality,
      resourcesPath: String = "\\$RESOURCES_DIR",
      setupCommands: String,
      preParse: String,
      parsers: String,
      postParse: String,
      postRun: String
    ) = {
    val mainResource = functionality.mainScript

    val extraImports =
      s"""# define helper functions
        |$quoteFunction
        |$removeFlagFunction""".stripMargin
    val setupFunction =
      s"""function ViashSetup {
        |$setupCommands
        |}""".stripMargin

    // check whether the wd needs to be set to the resources dir
    val cdToResources =
      if (functionality.set_wd_to_resources_dir.getOrElse(false)) {
        "\ncd \"" + resourcesPath + "\""
      } else {
        ""
      }

    // DETERMINE HOW TO RUN THE CODE
    val executionCode = mainResource match {
      case None => ""
      case Some(e: Executable) => e.path.get + " $VIASHARGS"
      case Some(res) => {
        s"""
          |set -e
          |tempscript=\\$$(mktemp /tmp/viash-run-${functionality.name}-XXXXXX)
          |function clean_up {
          |  rm "\\$$tempscript"
          |}
          |trap clean_up EXIT
          |cat > "\\$$tempscript" << 'VIASHMAIN'
          |${escape(functionality.mainCodeWithArgParse.get).replaceAll("\\\\\\$RESOURCES_DIR", resourcesPath)}
          |VIASHMAIN$cdToResources
          |${res.command("\\$tempscript")} $$VIASHARGS
          |""".stripMargin
      }
    }

    // generate bash document
    val (heredocStart, heredocEnd) = mainResource match {
      case None => ("", "")
      case Some(e: Executable) => ("", "")
      case _ => ("cat << VIASHEOF | ", "\nVIASHEOF")
    }

    /* GENERATE BASH SCRIPT */
    s"""#!/usr/bin/env bash
      |
      |set -e
      |
      |# figure out the directory the viash executable is in, follow symlinks
      |SOURCE="$${BASH_SOURCE[0]}"
      |while [ -h "$$SOURCE" ]; do
      |  DIR="$$( cd -P "$$( dirname "$$SOURCE" )" >/dev/null 2>&1 && pwd )"
      |  SOURCE="$$(readlink "$$SOURCE")"
      |  [[ $$SOURCE != /* ]] && SOURCE="$$DIR/$$SOURCE"
      |done
      |RESOURCES_DIR="$$( cd -P "$$( dirname "$$SOURCE" )" >/dev/null 2>&1 && pwd )"
      |
      |$extraImports
      |$setupFunction
      |$preParse
      |
      |VIASHARGS=''
      |while [[ $$# -gt 0 ]]; do
      |    case "$$1" in
      |        ---setup)
      |            ViashSetup
      |            exit 0
      |            ;;
      |$parsers
      |        *)    # unknown option
      |            ${quoteSaves("VIASHARGS", "$1")}
      |            shift # past argument
      |            ;;
      |    esac
      |done
      |
      |$postParse
      |
      |$heredocStart$executor $executionCode$heredocEnd
      |
      |$postRun""".stripMargin
  }

}
