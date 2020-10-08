package com.dataintuitive.viash.functionality

import dataobjects._
import resources._
import com.dataintuitive.viash.config.Version

case class Functionality(
  name: String,
  author: Option[String] = None,
  namespace: Option[String],
  version: Option[Version] = None,
  resources: Option[List[Resource]] = None,
  description: Option[String] = None,
  function_type: Option[FunctionType] = None,
  arguments: List[DataObject[_]] = Nil,

  // dummy arguments are used for handling extra directory mounts in docker
  dummy_arguments: Option[List[DataObject[_]]] = None,
  tests: Option[List[Resource]] = None,
  set_wd_to_resources_dir: Option[Boolean] = None,
) {

  // check whether there are not multiple positional arguments with multiplicity >1
  // and if there is one, whether its position is last
  {
    val positionals = arguments.filter(a => a.otype == "")
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

  def argumentsAndDummies: List[DataObject[_]] = arguments ::: dummy_arguments.getOrElse(Nil)
}

sealed trait FunctionType

case object AsIs extends FunctionType

case object Convert extends FunctionType

case object ToDir extends FunctionType

case object Join extends FunctionType
