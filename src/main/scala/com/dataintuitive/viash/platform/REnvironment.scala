package com.dataintuitive.viash.platform

case class REnvironment(
  packages: Option[List[String]] = None,
  github: Option[List[String]] = None
)