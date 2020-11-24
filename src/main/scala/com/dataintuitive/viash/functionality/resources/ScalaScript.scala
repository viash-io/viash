package com.dataintuitive.viash.functionality.resources

import com.dataintuitive.viash.functionality._
import com.dataintuitive.viash.functionality.dataobjects._

case class ScalaScript(
  path: Option[String] = None,
  text: Option[String] = None,
  dest: Option[String] = None,
  is_executable: Option[Boolean] = Some(true)
) extends Script {
  val `type` = "scala_script"
  val meta = ScalaScript
  def copyResource(path: Option[String], text: Option[String], dest: Option[String], is_executable: Option[Boolean]): Resource = {
    copy(path = path, text = text, dest = dest, is_executable = is_executable)
  }

  def generatePlaceholder(functionality: Functionality): String = {
    val params = functionality.arguments.filter(d => d.direction == Input || d.isInstanceOf[FileObject])

    val classTypes = params.map { par =>
      val classType = par match {
        case o: BooleanObject if o.multiple => "List[Boolean]"
        case o: IntegerObject if o.multiple => "List[Integer]"
        case o: DoubleObject if o.multiple => "List[Double]"
        case o: FileObject if o.multiple => "List[String]"
        case o: StringObject if o.multiple => "List[String]"
        // we could argue about whether these should be options or not
        case o: BooleanObject if !o.required && o.flagValue.isEmpty => "Option[Boolean]"
        case o: IntegerObject if !o.required => "Option[Integer]"
        case o: DoubleObject if !o.required => "Option[Double]"
        case o: FileObject if !o.required => "Option[String]"
        case o: StringObject if !o.required => "Option[String]"
        case _: BooleanObject => "Boolean"
        case _: IntegerObject => "Integer"
        case _: DoubleObject => "Double"
        case _: FileObject => "String"
        case _: StringObject => "String"
      }
      par.plainName + ": " + classType

    }

    val parSet = params.map { par =>
      val env_name = par.VIASH_PAR

      val parse = { par match {
        case o: BooleanObject if o.multiple =>
          s""""$$$env_name".split("${o.multiple_sep}").map(_.toLowerCase.toBoolean).toList"""
        case o: IntegerObject if o.multiple =>
          s""""$$$env_name".split("${o.multiple_sep}").map(_.toInt).toList"""
        case o: DoubleObject if o.multiple =>
          s""""$$$env_name".split("${o.multiple_sep}").map(_.toDouble).toList"""
        case o: FileObject if o.multiple =>
          s""""$$$env_name".split("${o.multiple_sep}").toList"""
        case o: StringObject if o.multiple =>
          s""""$$$env_name".split("${o.multiple_sep}").toList"""
        case o: BooleanObject if !o.required && o.flagValue.isEmpty => s"""Some("$$$env_name".toLowerCase.toBoolean)"""
        case o: IntegerObject if !o.required => s"""Some("$$$env_name".toInt)"""
        case o: DoubleObject if !o.required => s"""Some("$$$env_name".toDouble)"""
        case o: FileObject if !o.required => s"""Some("$$$env_name")"""
        case o: StringObject if !o.required => s"""Some("$$$env_name")"""
        case _: BooleanObject => s""""$$$env_name".toLowerCase.toBoolean"""
        case _: IntegerObject => s""""$$$env_name".toInt"""
        case _: DoubleObject => s""""$$$env_name".toDouble"""
        case _: FileObject => s""""$$$env_name""""
        case _: StringObject => s""""$$$env_name""""
      }}

      val notFound = par match {
        case o: DataObject[_] if o.multiple => Some("Nil")
        case o: BooleanObject if o.flagValue.isDefined => None
        case o: DataObject[_] if !o.required => Some("None")
        case _: DataObject[_] => None
      }

      notFound match {
        case Some(nf) =>
          s"""$$VIASH_DOLLAR$$( if [ ! -z $${$env_name+x} ]; then echo "${parse.replaceAll("\"", "\"'\"'\"")}"; else echo "$nf"; fi )"""
        case None => parse
      }
    }
    s"""case class ViashPar(
       |  ${classTypes.mkString(",\n  ")}
       |)
       |
       |val par = ViashPar(
       |  ${parSet.mkString(",\n  ")}
       |)
       |
       |val resources_dir = "$$VIASH_RESOURCES_DIR"
       |""".stripMargin
  }
}

object ScalaScript extends ScriptObject {
  val commentStr = "//"
  val extension = "scala"

  def command(script: String): String = {
    "scala -nc \"" + script + "\""
  }

  def commandSeq(script: String): Seq[String] = {
    Seq("scala", "-nc", script)
  }
}