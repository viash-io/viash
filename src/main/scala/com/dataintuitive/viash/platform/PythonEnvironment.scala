package com.dataintuitive.viash.platform

case class PythonEnvironment(
  packages: Option[List[String]] = None,
  github: Option[List[String]] = None
)