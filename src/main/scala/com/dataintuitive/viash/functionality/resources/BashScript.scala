package com.dataintuitive.viash.functionality.resources

import com.dataintuitive.viash.functionality._
import com.dataintuitive.viash.functionality.dataobjects._
import com.dataintuitive.viash.helpers.BashHelper

case class BashScript(
  name: Option[String] = None,
  path: Option[String] = None,
  text: Option[String] = None,
  is_executable: Boolean = true
) extends Script {
  val `type` = "bash_script"
  val commentStr = "#"

  def command(script: String) = {
    "bash \"" + script + "\""
  }
  def commandSeq(script: String) = {
    Seq("bash", script)
  }

  private def removeNewlines(s: String) = {
      s.filter(_ >= ' ') // remove all control characters
  }

  def generatePlaceholder(functionality: Functionality): String = {
    val params = functionality.arguments.filter(d => d.direction == Input || d.isInstanceOf[FileObject])

    val par_set = params.map{ par =>
      s"""${par.par}='$$${par.VIASH_PAR}'"""
    }
    s"""${par_set.mkString("\n")}
      |
      |resources_dir="$$VIASH_RESOURCES_DIR"
      |""".stripMargin
  }
}
