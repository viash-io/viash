package com.dataintuitive.viash.platforms.docker

sealed class DockerSetupStrategy(val id: String, val synonyms: List[String] = Nil)

case object AlwaysBuild extends DockerSetupStrategy("alwaysbuild", List("build"))
case object AlwaysPull extends DockerSetupStrategy("alwayspull", List("pull"))
case object AlwaysPullElseBuild extends DockerSetupStrategy("alwayspullelsebuild", List("pullelsebuild"))
case object AlwaysPullElseCachedBuild extends DockerSetupStrategy("alwayspullelsecachedbuild", List("pullelsecachedbuild"))
case object AlwaysCachedBuild extends DockerSetupStrategy("alwayscachedbuild", List("cachedbuild"))
case object IfNeedBeBuild extends DockerSetupStrategy("ifneedbebuild")
case object IfNeedBeCachedBuild extends DockerSetupStrategy("ifneedbecachedbuild")
case object IfNeedBePull extends DockerSetupStrategy( "idneedbepull")
case object IfNeedBePullElseBuild extends DockerSetupStrategy("ifneedbepullelsebuild")
case object IfNeedBePullElseCachedBuild extends DockerSetupStrategy("ifneedbepullelsecachedbuild")
case object DoNothing extends DockerSetupStrategy("donothing", List("meh"))

object DockerSetupStrategy {
  val objs: List[DockerSetupStrategy] = List(
    AlwaysBuild,
    AlwaysPull,
    AlwaysPullElseBuild,
    AlwaysPullElseCachedBuild,
    AlwaysCachedBuild,
    IfNeedBeBuild,
    IfNeedBeCachedBuild,
    IfNeedBePull,
    IfNeedBePullElseBuild,
    IfNeedBePullElseCachedBuild,
    DoNothing
  )

  val map: Map[String, DockerSetupStrategy] =
    objs.flatMap{obj =>
      (obj.id → obj) :: obj.synonyms.map(_ → obj)
    }.toMap
}