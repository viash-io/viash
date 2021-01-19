package com.dataintuitive.viash.platforms

import com.dataintuitive.viash.functionality.Functionality
import com.dataintuitive.viash.functionality.resources._
import com.dataintuitive.viash.platforms.requirements._
import com.dataintuitive.viash.config.Version
import com.dataintuitive.viash.wrapper.BashWrapper

case class NativePlatform(
  id: String = "native",
  setup: List[Requirements] = Nil
) extends Platform {
  val `type` = "native"

  val requirements: List[Requirements] = setup

  def modifyFunctionality(functionality: Functionality): Functionality = {
    val executor = functionality.mainScript match {
      case None => "eval"
      case Some(_: Executable) => "eval"
      case Some(_) => "bash"
    }

    // create new bash script
    val bashScript = BashScript(
      dest = Some(functionality.name),
      text = Some(BashWrapper.wrapScript(
        executor = executor,
        functionality = functionality,
        setupCommands = setupCommands
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
          "cat << 'HERE'\n" +
          "# run the following commands:\n" +
          li.mkString("", " && \\\n  ", "\n") +
          "HERE\n"
        }
      ).mkString

    s"""function ViashSetup {
       |${if (commands == "") ":\n" else commands}}""".stripMargin
  }
}
