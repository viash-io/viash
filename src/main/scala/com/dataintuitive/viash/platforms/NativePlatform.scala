package com.dataintuitive.viash.platforms

import com.dataintuitive.viash.functionality.Functionality
import com.dataintuitive.viash.functionality.resources._
import com.dataintuitive.viash.helpers.BashWrapper
import com.dataintuitive.viash.platforms.requirements._
import com.dataintuitive.viash.config.Version

case class NativePlatform(
  id: String = "native",
  version: Option[Version] = None,
  r: Option[RRequirements] = None,
  python: Option[PythonRequirements] = None
) extends Platform {
  val `type` = "native"

  val requirements: List[Requirements] =
    r.toList :::
    python.toList

  def modifyFunctionality(functionality: Functionality): Functionality = {
    val executor = functionality.mainScript match {
      case None => "eval"
      case Some(_: Executable) => "eval"
      case Some(_) => "bash"
    }

    // create new bash script
    val bashScript = BashScript(
      name = Some(functionality.name),
      text = Some(BashWrapper.wrapScript(
        executor = executor,
        functionality = functionality,
        setupCommands = setupCommands,
        preParse = "",
        parsers = "",
        postParse = "",
        postRun = ""
      ))
    )

    functionality.copy(
      resources = Some(bashScript :: functionality.resources.getOrElse(Nil).tail)
    )
  }

  def setupCommands: String = {
    val runCommands = requirements.map(_.installCommands)

    val commands =
      runCommands.map(li =>
        if (li.isEmpty) {
          ""
        } else {
          li.mkString("", " && \\\n  ", "\n")
        }
      ).mkString

    s"""function ViashSetup {
       |${if (commands == "") ":" else commands}
       |}""".stripMargin
  }
}
