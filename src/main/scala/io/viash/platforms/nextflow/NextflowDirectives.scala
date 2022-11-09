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

package io.viash.platforms.nextflow

import io.viash.helpers.data_structures._

// todo: assert contents?
case class NextflowDirectives(
  accelerator: Map[String, String] = Map(),
  afterScript: Option[String] = None,
  beforeScript: Option[String] = None,
  cache: Option[Either[Boolean, String]] = None,
  conda: OneOrMore[String] = Nil,
  container: Option[Either[Map[String, String], String]] = None, // TODO: need to implement container class?
  containerOptions: OneOrMore[String] = Nil,
  cpus: Option[Either[Int, String]] = None,
  disk: Option[String] = None,
  echo: Option[Either[Boolean, String]] = None,
  errorStrategy: Option[String] = None,
  executor: Option[String] = None,
  label: OneOrMore[String] = Nil,
  machineType: Option[String] = None,
  maxErrors: Option[Either[String, Int]] = None,
  maxForks: Option[Either[String, Int]] = None,
  maxRetries: Option[Either[String, Int]] = None,
  memory: Option[String] = None,
  module: OneOrMore[String] = Nil,
  penv: Option[String] = None,
  pod: OneOrMore[Map[String, String]] = Nil,
  publishDir: OneOrMore[Either[String, Map[String, String]]] = Nil, // TODO: need to implement publishdir class?
  queue: OneOrMore[String] = Nil,
  scratch: Option[Either[Boolean, String]] = None,
  storeDir: Option[String] = None,
  stageInMode: Option[String] = None,
  stageOutMode: Option[String] = None,
  tag: Option[String] = None,
  time: Option[String] = None
)