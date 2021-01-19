package com.dataintuitive.viash.helpers

import com.dataintuitive.viash.functionality.Functionality

case class DockerImageInfo(name: String, tag: Option[String] = None, registry: Option[String] = None) {
  override def toString = {
    registry.map(_ + "/").getOrElse("") +
      name + ":" +
      tag.getOrElse("latest")
  }
}

object Docker {
  def getImageInfo(
    fun: Functionality,
    customName: Option[String] = None,
    customNamespace: Option[String] = None,
    customRegistry: Option[String] = None,
    customVersion: Option[String] = None
  ) = {
    val name =
      (customNamespace orElse fun.namespace).map(_ + "/").getOrElse("") +
        customName.getOrElse(fun.name)

    DockerImageInfo(
      name = name,
      tag = (customVersion orElse fun.version.map(_.toString)),
      registry = customRegistry
    )
  }
}
