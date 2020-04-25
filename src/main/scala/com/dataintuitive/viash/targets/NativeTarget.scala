package com.dataintuitive.viash.targets

import com.dataintuitive.viash.functionality.{Functionality, Resource}
import com.dataintuitive.viash.functionality.platforms.BashPlatform
import com.dataintuitive.viash.helpers.BashHelper
import com.dataintuitive.viash.targets.environments._

case class NativeTarget(
  r: Option[REnvironment] = None,
  python: Option[PythonEnvironment] = None
) extends Target {
  val `type` = "native"

  def modifyFunctionality(functionality: Functionality) = {
    // create new bash script
    val bashScript = Resource(
        name = functionality.name,
        code = Some(BashHelper.wrapScript(
          executor = "bash",
          functionality = functionality,
          setupCommands = setupCommands,
          preParse = "",
          parsers = "",
          postParse = "",
          postRun = ""
        )),
        isExecutable = true
      )

    functionality.copy(
      resources = functionality.resources.filterNot(_.name.startsWith("main")) :::
        List(bashScript)
    )
  }

  def setupCommands = {
    val rInstallCommands = r.map(_.getInstallCommands()).getOrElse(Nil)
    val pythonInstallCommands = python.map(_.getInstallCommands()).getOrElse(Nil)

    val runCommands = List(rInstallCommands, pythonInstallCommands)
    val commands =
      runCommands.map(li =>
        if (li.isEmpty) {
          ""
        } else {
          li.mkString("", " && \\\n  ", "\n")
        }
      ).mkString

    if (commands == "") "echo Done\n" else commands
  }
}
