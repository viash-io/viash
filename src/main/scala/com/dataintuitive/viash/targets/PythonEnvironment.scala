package com.dataintuitive.viash.targets

case class PythonEnvironment(
  packages: Option[List[String]] = None,
  github: Option[List[String]] = None
)