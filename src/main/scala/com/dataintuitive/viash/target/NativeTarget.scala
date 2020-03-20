package com.dataintuitive.viash.target

case class NativeTarget(
  r: Option[REnvironment] = None,
  python: Option[PythonEnvironment] = None
) extends Target {
  val `type` = "native"
}