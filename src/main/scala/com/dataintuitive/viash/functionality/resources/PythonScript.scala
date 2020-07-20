package com.dataintuitive.viash.functionality.resources

import com.dataintuitive.viash.functionality._
import com.dataintuitive.viash.functionality.dataobjects._

case class PythonScript(
  name: Option[String] = None,
  path: Option[String] = None,
  text: Option[String] = None,
  is_executable: Boolean = true
) extends Script {
  val `type` = "python_script"

  val commentStr = "#"

  def command(script: String) = {
    "python \"" + script + "\""
  }
  def commandSeq(script: String) = {
    Seq("python", script)
  }

  def generatePlaceholder(functionality: Functionality): String = {
    val params = functionality.arguments.filter(d => d.direction == Input || d.isInstanceOf[FileObject])

    val par_set = params.map{ par =>
      val env_name = par.VIASH_PAR

      val parse = par match {
        case o: BooleanObject if o.multiple =>
          s"""list(map(lambda x: (x.lower() == 'true'), '$$$env_name'.split('${o.multiple_sep}')))"""
        case o: IntegerObject if o.multiple =>
          s"""list(map(int, '$$$env_name'.split('${o.multiple_sep}')))"""
        case o: DoubleObject if o.multiple =>
          s"""list(map(float, '$$$env_name'.split('${o.multiple_sep}')))"""
        case o: FileObject if o.multiple =>
          s"""'$$$env_name'.split('${o.multiple_sep}')"""
        case o: StringObject if o.multiple =>
          s"""'$$$env_name'.split('${o.multiple_sep}')"""
        case o: BooleanObject => s"""'$$$env_name'.lower() == 'true'"""
        case o: IntegerObject => s"""int('$$$env_name')"""
        case o: DoubleObject => s"""float('$$$env_name')"""
        case o: FileObject => s"""'$$$env_name'"""
        case o: StringObject => s"""'$$$env_name'"""
      }

      s"""'${par.plainName}': $$VIASH_DOLLAR$$( if [ ! -z $${$env_name+x} ]; then echo "$parse"; else echo None; fi )"""
    }
    s"""par = {
      |  ${par_set.mkString(",\n  ")}
      |}
      |
      |resources_dir = '$$VIASH_RESOURCES_DIR'
      |""".stripMargin
  }
}
