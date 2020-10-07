package com.dataintuitive.viash.functionality.resources

import com.dataintuitive.viash.functionality._
import com.dataintuitive.viash.functionality.dataobjects._

case class RScript(
  name: Option[String] = None,
  path: Option[String] = None,
  text: Option[String] = None,
  is_executable: Boolean = true
) extends Script {
  val `type` = "r_script"
  val meta = RScript

  def generatePlaceholder(functionality: Functionality): String = {
    val params = functionality.arguments.filter(d => d.direction == Input || d.isInstanceOf[FileObject])

    val par_set = params.map { par =>
      val env_name = par.VIASH_PAR

      val parse = par match {
        case o: BooleanObject if o.multiple =>
          s"""as.logical(strsplit(toupper('$$$env_name'), split = '${o.multiple_sep}')[[1]])"""
        case o: IntegerObject if o.multiple =>
          s"""as.integer(strsplit('$$$env_name', split = '${o.multiple_sep}')[[1]])"""
        case o: DoubleObject if o.multiple =>
          s"""as.numeric(strsplit('$$$env_name', split = '${o.multiple_sep}')[[1]])"""
        case o: FileObject if o.multiple =>
          s"""strsplit('$$$env_name', split = '${o.multiple_sep}')[[1]]"""
        case o: StringObject if o.multiple =>
          s"""strsplit('$$$env_name', split = '${o.multiple_sep}')[[1]]"""
        case _: BooleanObject => s"""as.logical(toupper('$$$env_name'))"""
        case _: IntegerObject => s"""as.integer($$$env_name)"""
        case _: DoubleObject => s"""as.numeric($$$env_name)"""
        case _: FileObject => s"""'$$$env_name'"""
        case _: StringObject => s"""'$$$env_name'"""
      }

      s""""${par.plainName}" = $$VIASH_DOLLAR$$( if [ ! -z $${$env_name+x} ]; then echo "$parse"; else echo NULL; fi )"""
    }
    s"""par <- list(
       |  ${par_set.mkString(",\n  ")}
       |)
       |
       |resources_dir = "$$VIASH_RESOURCES_DIR"
       |""".stripMargin
  }
}

object RScript extends ScriptObject {
  val commentStr = "#"
  val extension = "R"

  def command(script: String): String = {
    "Rscript \"" + script + "\""
  }

  def commandSeq(script: String): Seq[String] = {
    Seq("Rscript", script)
  }
}