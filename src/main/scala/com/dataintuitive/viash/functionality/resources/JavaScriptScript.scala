package com.dataintuitive.viash.functionality.resources

import com.dataintuitive.viash.functionality._
import com.dataintuitive.viash.functionality.dataobjects._

case class JavaScriptScript(
  path: Option[String] = None,
  text: Option[String] = None,
  dest: Option[String] = None,
  is_executable: Boolean = true
) extends Script {
  val `type` = "javascript_script"
  val meta = JavaScriptScript
  def copyResource(path: Option[String], text: Option[String], dest: Option[String], is_executable: Boolean): Resource = {
    copy(path = path, text = text, dest = dest, is_executable = is_executable)
  }

  def generatePlaceholder(functionality: Functionality): String = {
    val params = functionality.arguments.filter(d => d.direction == Input || d.isInstanceOf[FileObject])

    val par_set = params.map { par =>
      val env_name = par.VIASH_PAR

      val parse = par match {
        case o: BooleanObject if o.multiple =>
          s"""'$$$env_name'.split('${o.multiple_sep}').map(x => x.toLowerCase() === 'true')"""
        case o: IntegerObject if o.multiple =>
          s"""'$$$env_name'.split('${o.multiple_sep}').map(x => parseInt(x))"""
        case o: DoubleObject if o.multiple =>
          s"""'$$$env_name'.split('${o.multiple_sep}').map(x => parseFloat(x))"""
        case o: FileObject if o.multiple =>
          s"""'$$$env_name'.split('${o.multiple_sep}')"""
        case o: StringObject if o.multiple =>
          s"""'$$$env_name'.split('${o.multiple_sep}')"""
        case _: BooleanObject => s"""'$$$env_name'.toLowerCase() === 'true'"""
        case _: IntegerObject => s"""parseInt('$$$env_name')"""
        case _: DoubleObject => s"""parseFloat('$$$env_name')"""
        case _: FileObject => s"""'$$$env_name'"""
        case _: StringObject => s"""'$$$env_name'"""
      }

      s"""'${par.plainName}': $$VIASH_DOLLAR$$( if [ ! -z $${$env_name+x} ]; then echo "$parse"; else echo undefined; fi )"""
    }
    s"""let par = {
       |  ${par_set.mkString(",\n  ")}
       |};
       |
       |let resources_dir = '$$VIASH_RESOURCES_DIR'
       |""".stripMargin
  }
}

object JavaScriptScript extends ScriptObject {
  val commentStr = "//"
  val extension = "js"

  def command(script: String): String = {
    "node \"" + script + "\""
  }

  def commandSeq(script: String): Seq[String] = {
    Seq("node", script)
  }
}
