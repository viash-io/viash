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

package com.dataintuitive.viash.platforms.docker

sealed class DockerPushStrategy(val id: String, val synonyms: List[String] = Nil)

case object AlwaysPush extends DockerPushStrategy("alwayspush", List("forcepush"))
case object PushIfNotPresent extends DockerPushStrategy("pushifnotpresent", List("gentlepush"))
case object NoPush extends DockerPushStrategy("donothing", List("meh"))

object DockerPushStrategy {
  val objs: List[DockerPushStrategy] = List(
    AlwaysPush,
    PushIfNotPresent,
    NoPush
  )

  val map: Map[String, DockerPushStrategy] =
    objs.flatMap{obj =>
      (obj.id → obj) :: obj.synonyms.map(_ → obj)
    }.toMap
}
