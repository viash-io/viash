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

    val execute_bash = Resource(
      name = "execute.sh",
      code = Some(s"""#!/bin/bash
        |
        |$command "$$@"
      """.stripMargin),
      isExecutable = Some(true)
    )

    val execute_batch = Resource(
      name = "execute.bat",
      code = Some(s"$command %*")
    )

    // create setup scripts
    val rInstallCommands = r.map(_.getInstallCommands()).getOrElse(Nil)
    val pythonInstallCommands = python.map(_.getInstallCommands()).getOrElse(Nil)

    val setup_bash = Resource(
      name = "setup.sh",
      code = Some(s"""#!/bin/bash
        |${if (!rInstallCommands.isEmpty) "\n# install R requirements" else ""}
        |${rInstallCommands.mkString(" && \\\n  ")}
        |${if (!pythonInstallCommands.isEmpty) "\n# install Python requirements" else ""}
        |${pythonInstallCommands.mkString(" && \\\n  ")}
      """.stripMargin),
      isExecutable = Some(true)
    )

    val setup_batch = Resource(
      name = "setup.bat",
      code = Some(s"""${rInstallCommands.mkString(" && \\\n  ")}
        |
        |${pythonInstallCommands.mkString(" && \\\n  ")}
      """.stripMargin)
    )

    functionality.copy(
      resources = 
        functionality.resources.filterNot(_.name.startsWith("main")) ::: 
          List(newMainResource, execute_bash, execute_batch, setup_bash, setup_batch)
    )
  }
}
