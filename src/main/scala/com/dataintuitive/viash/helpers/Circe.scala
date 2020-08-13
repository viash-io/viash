package com.dataintuitive.viash.helpers

import io.circe.generic.extras.Configuration

object Circe {
  implicit val customConfig: Configuration = Configuration.default.withDefaults
}