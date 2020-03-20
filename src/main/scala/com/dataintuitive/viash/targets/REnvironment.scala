package com.dataintuitive.viash.target

case class REnvironment(
  packages: Option[List[String]] = None,
  github: Option[List[String]] = None
)