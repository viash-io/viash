package com.dataintuitive.viash.functionality.resources

import com.dataintuitive.viash.functionality._
import com.dataintuitive.viash.functionality.dataobjects._

case class BashScript(
  path: Option[String] = None,
  text: Option[String] = None,
  dest: Option[String] = None,
  is_executable: Option[Boolean] = Some(true)
) extends Script {
  val `type` = "bash_script"
  val meta = BashScript
  def copyResource(path: Option[String], text: Option[String], dest: Option[String], is_executable: Option[Boolean]): Resource = {
    copy(path = path, text = text, dest = dest, is_executable = is_executable)
  }

  def generatePlaceholder(functionality: Functionality): String = {
    val params = functionality.arguments.filter(d => d.direction == Input || d.isInstanceOf[FileObject])

    val par_set = params.map { par =>
      s"""${par.par}='$$${par.VIASH_PAR}'"""
    }
    s"""${par_set.mkString("\n")}
       |
       |resources_dir="$$VIASH_RESOURCES_DIR"
       |""".stripMargin
  }
}

object BashScript extends ScriptObject {
  val commentStr = "#"
  val extension = "sh"

  def command(script: String): String = {
    "bash \"" + script + "\""
  }

  def commandSeq(script: String): Seq[String] = {
    Seq("bash", script)
  }
}
