package com.dataintuitive.viash.functionality

import dataobjects._
import resources._
import com.dataintuitive.viash.config.Version

case class Functionality(
  name: String,
  namespace: Option[String] = None,
  version: Option[Version] = None,
  authors: List[Author] = Nil,
  arguments: List[DataObject[_]] = Nil,
  resources: Option[List[Resource]] = None,
  description: Option[String] = None,
  function_type: Option[FunctionType] = None,
  tests: Option[List[Resource]] = None,

  // dummy arguments are used for handling extra directory mounts in docker
  dummy_arguments: Option[List[DataObject[_]]] = None,

  // setting this to true will change the working directory
  // to the resources directory when running the script
  // this is used when running `viash test`.
  set_wd_to_resources_dir: Option[Boolean] = None
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
