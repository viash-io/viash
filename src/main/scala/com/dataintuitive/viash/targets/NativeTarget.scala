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
    
    val setup_bash = Resource(
      name = "setup.sh",
      code = Some("")
    )
    
    val setup_batch = Resource(
      name = "setup.bat",
      code = Some("")
    )
    
    List(execute_bash, execute_batch, setup_bash, setup_batch)
  }
}