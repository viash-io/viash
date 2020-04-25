package com.dataintuitive.viash.helpers

import com.dataintuitive.viash.functionality._
import com.dataintuitive.viash.functionality.platforms._
import java.nio.file.Paths

object BashHelper {
  val quoteFunction = {
    """function ViashQuote {
      |  echo $1 | sed "s/'/'\"'\"'/g" | sed "s/^[^-].*/'&'/"
      |}""".stripMargin
  }
  val removeFlagFunction = {
    """function ViashRemoveFlags {
      |  echo $1 | sed 's/^[^=]*=//'
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
    str.replaceAll("([\\$`])", "\\\\$1")
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
      resourcesPath: String,
      setupCommands: String,
      preParse: String,
      parsers: String,
      postParse: String
    ) = {
    val mainResource = functionality.mainResource.get

    val extraImports =
      s"""# define helper functions
        |$quoteFunction
        |$removeFlagFunction""".stripMargin
    val setupFunction =
      s"""function ViashSetup {
        |$setupCommands
        |}""".stripMargin

    // DETERMINE HOW TO RUN THE CODE
    val code = ""

     // get main script
    val mainPath = Paths.get(resourcesPath, mainResource.name).toFile().getPath()

    val executionCode = functionality.platform match {
      case NativePlatform =>
        mainResource.path.map(_ + " $VIASHARGS").getOrElse("echo No command provided")
      /*case BashPlatform =>
        s"""
          |set -- $$VIASHARGS
          |${BashHelper.escape(fun2.mainCodeWithArgParse.get)}
          |""".stripMargin*/
      case pl => {
        s"""
          |if [ ! -d "$resourcesPath" ]; then mkdir "$resourcesPath"; fi
          |cat > "$mainPath" << 'VIASHMAIN'
          |${BashHelper.escape(functionality.mainCodeWithArgParse.get)}
          |VIASHMAIN
          |${pl.command(mainPath)} $$VIASHARGS
          |""".stripMargin
      }
    }

    // generate bash document
    val (heredocStart, heredocEnd) = functionality.platform match {
      case NativePlatform => ("", "")
      case _ => ("cat << VIASHEOF | ", "\nVIASHEOF")
    }

    /* GENERATE BASH SCRIPT */
    s"""#!/bin/bash
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
      |$heredocStart$executor $executionCode$heredocEnd""".stripMargin
  }
}