package com.dataintuitive.viash.target

case class DockerTarget(
  image: String,
  volumes: Option[Seq[Volume]] = None,
  port: Option[String] = None,
  workdir: Option[String] = None,
  r: Option[REnvironment] = None,
  python: Option[PythonEnvironment] = None
) extends Target {
  val `type` = "docker"
}

case class Volume(
  name: String,
  mount: String
)