/*
 * Copyright (C) 2020  Data Intuitive
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.viash.platforms.docker

sealed class DockerSetupStrategy(val id: String, val synonyms: List[String] = Nil)

case object AlwaysBuild extends DockerSetupStrategy("alwaysbuild", List("build", "b"))
case object AlwaysPull extends DockerSetupStrategy("alwayspull", List("pull", "p"))
case object AlwaysPullElseBuild extends DockerSetupStrategy("alwayspullelsebuild", List("pullelsebuild"))
case object AlwaysPullElseCachedBuild extends DockerSetupStrategy("alwayspullelsecachedbuild", List("pullelsecachedbuild"))
case object AlwaysCachedBuild extends DockerSetupStrategy("alwayscachedbuild", List("cachedbuild", "cb"))
case object IfNeedBeBuild extends DockerSetupStrategy("ifneedbebuild", Nil)
case object IfNeedBeCachedBuild extends DockerSetupStrategy("ifneedbecachedbuild", Nil)
case object IfNeedBePull extends DockerSetupStrategy( "ifneedbepull", Nil)
case object IfNeedBePullElseBuild extends DockerSetupStrategy("ifneedbepullelsebuild", Nil)
case object IfNeedBePullElseCachedBuild extends DockerSetupStrategy("ifneedbepullelsecachedbuild", Nil)
case object DoNothing extends DockerSetupStrategy("donothing", List("meh"))
case object Push extends DockerSetupStrategy("push", List("forcepush", "alwayspush"))
case object PushIfNotPresent extends DockerSetupStrategy("pushifnotpresent", List("gentlepush", "maybepush"))

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
    Push,
    PushIfNotPresent,
    DoNothing
  )

  val map: Map[String, DockerSetupStrategy] =
    objs.flatMap{obj =>
      (obj.id -> obj) :: obj.synonyms.map(_ -> obj)
    }.toMap
}