package io.viash.executors

import io.viash.functionality.resources.{Resource, Script}

final case class ExecutorResources(
  mainScript: Option[Script],
  additionalResources: List[Resource]
) {
  def resources: List[Resource] = mainScript.toList ++ additionalResources
}
