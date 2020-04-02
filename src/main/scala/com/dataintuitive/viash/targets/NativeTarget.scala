package com.dataintuitive.viash.targets

import com.dataintuitive.viash.functionality.{Functionality, Resource}
import com.dataintuitive.viash.targets.environments._

case class NativeTarget(
  r: Option[REnvironment] = None,
  python: Option[PythonEnvironment] = None
) extends Target {
  val `type` = "native"

  def modifyFunctionality(functionality: Functionality) = {
    // create run scripts
    val mainResource = functionality.mainResource.get

    val newMainResource = functionality.platform match {
      case None =>
        mainResource
      case Some(pl) => 
        mainResource.copy(
          code = functionality.mainCodeWithArgParse,
          path = None
        )
    }

    val command = functionality.platform match {
      case None => mainResource.name
      case Some(pl) => pl.command(mainResource.name)
    }

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
    
    val bash = Resource(
      name = functionality.name,
      code = Some(s"""#!/bin/bash
        |if [ "$$1" = "---setup" ]; then$rInstallStr$pythonInstallStr
        |  exit 0
        |fi
        |
        |$command $$@
      """.stripMargin),
      isExecutable = true
    )

    functionality.copy(
      resources = 
        functionality.resources.filterNot(_.name.startsWith("main")) ::: 
        List(newMainResource, bash)
    )
  }
}
