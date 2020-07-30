package com.dataintuitive.viash.platforms

import com.dataintuitive.viash.functionality.{Functionality}
import com.dataintuitive.viash.functionality.resources._
import com.dataintuitive.viash.helpers.{BashHelper, BashWrapper}
import com.dataintuitive.viash.platforms.requirements._

case class NativePlatform(
  version: Option[String],
  r: Option[RRequirements] = None,
  python: Option[PythonRequirements] = None
) extends Platform {
  val `type` = "native"

  val requirements: List[Requirements] =
    r.toList :::
    python.toList

  def modifyFunctionality(functionality: Functionality) = {
    val executor = functionality.mainScript match {
      case None => "eval"
      case Some(e: Executable) => "eval"
      case Some(_) => "bash"
    }

    // create new bash script
    val bashScript = BashScript(
      name = Some(functionality.name),
      text = Some(BashWrapper.wrapScript(
        executor = executor,
        functionality = functionality,
        setupCommands = setupCommands,
        dockerfileCommands = "",
        preParse = "",
        parsers = "",
        postParse = "",
        postRun = ""
      )),
      is_executable = true
    )

    functionality.copy(
      resources = bashScript :: functionality.resources.tail
    )
  }

  def setupCommands = {
    val runCommands = requirements.map(_.installCommands)

    val commands =
      runCommands.map(li =>
        if (li.isEmpty) {
          ""
        } else {
          li.mkString("", " && \\\n  ", "\n")
        }
      ).mkString

    if (commands == "") ":" else commands
  }
}
