package com.dataintuitive.viash.targets

import com.dataintuitive.viash.functionality.{Functionality}
import com.dataintuitive.viash.functionality.resources._
import com.dataintuitive.viash.helpers.BashHelper
import com.dataintuitive.viash.targets.environments._

case class NativeTarget(
  r: Option[REnvironment] = None,
  python: Option[PythonEnvironment] = None
) extends Target {
  val `type` = "native"

  def modifyFunctionality(functionality: Functionality, test: Option[Script]) = {
    // create new bash script
    val text = test match {
      case None => BashHelper.wrapScript(
          executor = "bash",
          functionality = functionality,
          setupCommands = setupCommands,
          preParse = "",
          parsers = "",
          postParse = "",
          postRun = ""
        )
      case Some(t) => BashHelper.wrapTest(
          executor = "bash",
          functionality = functionality,
          setupCommands = setupCommands,
          test = t
        )
    }
    val bashScript = BashScript(
        name = Some(functionality.name),
        text = Some(text),
        is_executable = true
      )

    functionality.copy(
      resources = bashScript :: functionality.resources.tail
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

    if (commands == "") ":\n" else commands
  }
}
