package com.dataintuitive.viash.helpers

import com.dataintuitive.viash.functionality._
import com.dataintuitive.viash.functionality.resources._
import java.nio.file.Paths
import scala.io.Source

object BashHelper {
  private def readUtils(s: String) = {
    val path = s"com/dataintuitive/viash/helpers/bashutils/$s.sh"
    Source.fromResource(path).getLines().mkString("\n")
  }

  lazy val ViashQuote = readUtils("ViashQuote")
  lazy val ViashExtractFlags = readUtils("ViashExtractFlags")
  lazy val ViashRemoveFlags = readUtils("ViashRemoveFlags")
  lazy val ViashAbsolutePath = readUtils("ViashAbsolutePath")
  lazy val ViashAutodetectMount = readUtils("ViashAutodetectMount")

  // usage: `ViashSourceDir ${BASH_SOURCE[0]}`
  lazy val ViashSourceDir = readUtils("ViashSourceDir")

  def save(saveVariable: String, args: Seq[String]) = {
    saveVariable + "=\"$" + saveVariable + " " + args.mkString(" ") + "\""
  }

  // generate strings in the form of:
  // SAVEVARIABLE="$SAVEVARIABLE `Quote $arg1` `Quote $arg2`"
  def quoteSave(saveVariable: String, args: Seq[String]) = {
    saveVariable + "=\"$" + saveVariable +
      args.map(" $(ViashQuote \"" + _ + "\")").mkString +
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
    argStore(name + "=*", plainName, "$(ViashRemoveFlags \"$1\")", 1, storeUnparsed)
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
      |# define helper functions
      |$ViashQuote
      |$ViashRemoveFlags
      |$ViashSourceDir
      |
      |# find source folder of this component
      |RESOURCES_DIR=`ViashSourceDir $${BASH_SOURCE[0]}`
      |
      |# helper function for installing extra requirements for this component
      |function ViashSetup {
      |$setupCommands
      |}
      |${if (preParse != "") "\n" + preParse + "\n" else ""}
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
      |${if (postParse != "") "\n" + postParse + "\n" else ""}
      |$heredocStart$executor $executionCode$heredocEnd
      |${if (postRun != "") "\n" + postRun + "\n" else ""}""".stripMargin
  }

}
