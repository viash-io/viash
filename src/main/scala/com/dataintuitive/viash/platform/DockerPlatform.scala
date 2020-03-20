package com.dataintuitive.viash.platform

case class DockerPlatform(
  image: String,
  volumes: Option[Seq[Volume]] = None,
  port: Option[String] = None,
  workdir: Option[String] = None,
  r: Option[REnvironment] = None,
  python: Option[PythonEnvironment] = None
) extends Platform {
  val `type` = "docker"
}

case class Volume(
  name: String,
  mount: String
)