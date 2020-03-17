package com.dataintuitive.viash.platform

case class DockerPlatform(
  image: String,
  volumes: Option[List[Volume]] = None,
  port: Option[String] = None,
  workdir: Option[String] = None,
  r: Option[REnvironment] = None,
  python: Option[PythonEnvironment] = None
)

case class Volume(
  name: String,
  mount: String
)