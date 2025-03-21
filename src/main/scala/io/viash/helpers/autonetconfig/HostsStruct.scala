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

package io.viash.helpers.autonetconfig

case class HostsStruct(
  http: String,
  ssh: String,
  sshttp: String,
  images: String,
  sources: String,
  sources_type: SourcesType,
  // sources_type: String,
  front: String,
  back: String,
  back_protocol: Protocol
  // back_protocol: String
)

enum SourcesType:
  case Gitlab, Gitea

enum Protocol:
  case Http, Https, Auto

// sealed trait Protocol
// case object Http extends Protocol
// case object Https extends Protocol
// case object Auto extends Protocol

// sealed trait SourcesType
// case object Gitlab extends SourcesType
// case object Gitea extends SourcesType