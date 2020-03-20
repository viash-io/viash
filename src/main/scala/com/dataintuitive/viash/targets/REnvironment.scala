package com.dataintuitive.viash.targets

case class REnvironment(
  packages: Option[List[String]] = None,
  github: Option[List[String]] = None
)