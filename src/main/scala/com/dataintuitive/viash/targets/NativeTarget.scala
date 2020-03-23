package com.dataintuitive.viash.targets

import com.dataintuitive.viash.functionality.{Functionality, Resource}
import com.dataintuitive.viash.targets.environments.PythonEnvironment
import com.dataintuitive.viash.targets.environments.REnvironment

case class NativeTarget(
  r: Option[REnvironment] = None,
  python: Option[PythonEnvironment] = None
) extends Target {
  val `type` = "native"
  
  def modifyFunctionality(functionality: Functionality) = {
    // create run scripts
    val mainRes = functionality.resources.find(_.name.startsWith("main")).get
    
    val command = functionality.platform.command(mainRes.name)
    
    val execute_bash = Resource(
      name = "execute.sh",
      code = Some(s"""#!/bin/bash
        |
        |$command "$$@"
      """.stripMargin),
      executable = Some(true)
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
      executable = Some(true)
    )
    
    val setup_batch = Resource(
      name = "setup.bat",
      code = Some(s"""${rInstallCommands.mkString(" && \\\n  ")}
        |
        |${pythonInstallCommands.mkString(" && \\\n  ")}
      """.stripMargin)
    )
    
    functionality.copy(
      resources = functionality.resources ::: List(execute_bash, execute_batch, setup_bash, setup_batch)
    )
  }
}