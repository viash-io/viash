package com.dataintuitive.viash.targets

import com.dataintuitive.viash.functionality.{Functionality, Resource}
import com.dataintuitive.viash.functionality.platforms.BashPlatform
import com.dataintuitive.viash.targets.environments._

case class NativeTarget(
  r: Option[REnvironment] = None,
  python: Option[PythonEnvironment] = None
) extends Target {
  val `type` = "native"

  def setupCommands() = {
    // create setup scripts
    val rInstallCommands = r.map(_.getInstallCommands()).getOrElse(Nil)
    val rInstallStr = if (!rInstallCommands.isEmpty) {
      "\n  # install R requirements\n  " + rInstallCommands.mkString(" && \\\n    ")
    } else {
      ""
    }
    val pythonInstallCommands = python.map(_.getInstallCommands()).getOrElse(Nil)
    val pythonInstallStr = if (!pythonInstallCommands.isEmpty) {
      "\n  # install Python requirements\n  " + pythonInstallCommands.mkString(" && \\\n    ")
    } else {
      ""
    }

    s"""if [ "$$1" = "---setup" ]; then$rInstallStr$pythonInstallStr
        |  exit 0
        |fi""".stripMargin
  }

  def modifyFunctionality(functionality: Functionality) = {
    // create run scripts
    val mainResource = functionality.mainResource.get

    val newResources = functionality.platform match {
      case None => List(mainResource)
      case Some(BashPlatform) => {
        val code = functionality.mainCodeWithArgParse.get.split("\n")

        val newCode =
          code.takeWhile(_.startsWith("#!")).mkString("\n") +
          "\n\n" + setupCommands() + "\n" +
          code.dropWhile(_.startsWith("#!")).mkString("\n")

        List(Resource(
          name = functionality.name,
          code = Some(newCode),
          path = None,
          isExecutable = true
        ))
      }
      case Some(pl) => {
        val res1 = mainResource.copy(
          code = functionality.mainCodeWithArgParse,
          path = None
        )

        val command = functionality.platform match {
          case None => mainResource.name
          case Some(pl) => pl.command(mainResource.name)
        }

        val res2 = Resource(
          name = functionality.name,
          code = Some(s"""#!/bin/bash
            |
            |${setupCommands()}
            |
            |DIR=$$(dirname "$$0")
            |
            |$command "$$@"
          """.stripMargin),
          isExecutable = true
        )

        List(res1, res2)
      }
    }

    functionality.copy(
      resources =
        functionality.resources.filterNot(_.name.startsWith("main")) :::
        newResources
    )
  }
}
