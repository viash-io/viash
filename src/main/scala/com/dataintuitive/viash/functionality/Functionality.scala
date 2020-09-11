package com.dataintuitive.viash.functionality

import io.circe.yaml.parser
import java.nio.file.Paths
import dataobjects._
import resources._
import com.dataintuitive.viash.helpers.IO
import java.net.URI
import com.dataintuitive.viash.config.Version

case class Functionality(
  name: String,
  namespace: Option[String],
  version: Option[Version] = None,
  resources: Option[List[Resource]] = None,
  description: Option[String] = None,
  function_type: Option[FunctionType] = None,
  arguments: List[DataObject[_]] = Nil,
  tests: Option[List[Resource]] = None,
  set_wd_to_resources_dir: Option[Boolean] = None,
) {

  // check whether there are not multiple positional arguments with multiplicity >1
  // and if there is one, whether its position is last
  {
    val positionals = arguments.filter(_.otype == "")
    val multiix = positionals.indexWhere(_.multiple)

    require(
      multiix == -1 || multiix == positionals.length - 1,
      message = s"positional argument ${positionals(multiix).name} should be last since it has multiplicity >1"
    )
  }

  def mainScript: Option[Script] =
    resources.getOrElse(Nil).head match {
      case s: Script => Some(s)
      case _ => None
    }

  def mainCode: Option[String] = mainScript.flatMap(_.read)
}

object Functionality {
  def parse(uri: URI): Functionality = {
    val str = IO.read(uri)

    val fun = parser.parse(str)
      .fold(throw _, _.as[Functionality])
      .fold(throw _, identity)

    val resources = fun.resources.getOrElse(Nil).map(makeResourcePathAbsolute(_, uri))
    val tests = fun.tests.getOrElse(Nil).map(makeResourcePathAbsolute(_, uri))

    fun.copy(
      resources = Some(resources),
      tests = Some(tests)
    )
  }

  def makeResourcePathAbsolute(res: Resource, parent: URI): Resource = {
    if (res.isInstanceOf[Executable] || res.path.isEmpty || res.path.get.contains("://")) {
        res
      } else {
        val p = Paths.get(res.path.get).toFile
        if (p.isAbsolute) {
          res
        } else {
          val newPath = Some(parent.resolve(res.path.get).toString)
          res match {
            case s: BashScript => s.copy(path = newPath)
            case s: PythonScript => s.copy(path = newPath)
            case s: RScript => s.copy(path = newPath)
            case f: PlainFile => f.copy(path = newPath)
          }
        }
      }
  }

  def read(path: String): Functionality = {
    parse(IO.uri(path))
  }
}

sealed trait FunctionType
case object AsIs extends FunctionType
case object Convert extends FunctionType
case object ToDir extends FunctionType
case object Join extends FunctionType
