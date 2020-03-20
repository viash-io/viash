package com.dataintuitive.viash.targets

import com.dataintuitive.viash.functionality.{Functionality, Resource}

case class DockerTarget(
  image: String,
  volumes: Option[Seq[Volume]] = None,
  port: Option[String] = None,
  workdir: Option[String] = None,
  r: Option[REnvironment] = None,
  python: Option[PythonEnvironment] = None
) extends Target {
  val `type` = "docker"
  
  def setupResources(functionality: Functionality) = {
    List()
  }
}

case class Volume(
  name: String,
  mount: String
)