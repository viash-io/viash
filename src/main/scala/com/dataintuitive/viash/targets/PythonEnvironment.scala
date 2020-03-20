package com.dataintuitive.viash.target

case class PythonEnvironment(
  packages: Option[List[String]] = None,
  github: Option[List[String]] = None
)