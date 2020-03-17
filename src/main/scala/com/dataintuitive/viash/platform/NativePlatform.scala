package com.dataintuitive.viash.platform

case class NativePlatform(
  r: Option[REnvironment] = None,
  python: Option[PythonEnvironment] = None
)