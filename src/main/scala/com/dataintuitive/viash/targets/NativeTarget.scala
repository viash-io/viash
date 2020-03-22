package com.dataintuitive.viash.targets

import com.dataintuitive.viash.functionality.{Functionality, Resource}

case class NativeTarget(
  r: Option[REnvironment] = None,
  python: Option[PythonEnvironment] = None
) extends Target {
  val `type` = "native"
  
  def setupResources(functionality: Functionality) = {
    val mainRes = functionality.resources.find(_.name.startsWith("main")).get
    
    val command = functionality.platform.command(mainRes.name)
    
    val execute_bash = Resource(
      name = "execute.sh",
      code = Some(s"""#!/bin/bash
        |
        |$command "$$@"
      """.stripMargin)
    )
    
    val execute_batch = Resource(
      name = "execute.bat",
      code = Some(s"$command %*")
    )
    
    val rInstallCommands = r.map(_.getInstallCommands()).getOrElse(Nil)
    val pythonInstallCommands = python.map(_.getInstallCommands()).getOrElse(Nil)
    
    val setup_bash = Resource(
      name = "setup.sh",
      code = Some(s"""#!/bin/bash
      |
      |# install R requirements
      |${rInstallCommands.mkString(" && \\\n  ")}
      |
      |# install R requirements
      |${pythonInstallCommands.mkString(" && \\\n  ")}
      """.stripMargin)
    )
    
    val setup_batch = Resource(
      name = "setup.bat",
      code = Some(s"""${rInstallCommands.mkString(" && \\\n  ")}
      |
      |${pythonInstallCommands.mkString(" && \\\n  ")}
      """.stripMargin)
    )
    
    List(execute_bash, execute_batch, setup_bash, setup_batch)
  }
}