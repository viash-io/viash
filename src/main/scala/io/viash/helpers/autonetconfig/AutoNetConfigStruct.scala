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

import java.net.URI
import java.nio.file.Paths
import io.viash.helpers.circe.Convert
import io.viash.helpers.{IO, SysEnv}
import io.viash.helpers.circe._
import io.viash.helpers.Logging

case class AutoNetConfigStruct(
  api_version: String,
  valid_until: String,
  provided_by: String,
  name: String,
  prefix: String,
  hosts: HostsStruct,
  validation: Map[String, Map[String, ValidationStruct]]
)

object AutoNetConfigStruct extends Logging {
  def fetchAnc(uri: String): Option[AutoNetConfigStruct] = {
    info(s"Fetching ANC from ${uri}")
    val txt = IO.readSome(URI(uri))
    if (txt.isEmpty) return None

    val json = Convert.textToJson(txt.get, uri)
    val anc = Convert.jsonToClass[AutoNetConfigStruct](json, uri)
    Some(anc)
  }
  def fetch(base: String): Option[AutoNetConfigStruct] = {

    // check if found in cache and not expired
    // if not, fetch from the network
    val path = Paths.get(SysEnv.viashHome).resolve("anc-cache").resolve(base)
    val cached = fetchAnc(path.toUri().toString())
    if (cached.isDefined) {
      val now = java.time.Instant.now().atZone(java.time.ZoneId.of("UTC")).toInstant()
      val validUntil = java.time.Instant.parse(cached.get.valid_until)
      if (now.isBefore(validUntil)) {
        return cached
      }
    }

    val res = fetchAnc(s"https://${base}/auto_net_config/auto_net_config")
      .orElse(fetchAnc(s"https://auto_net_config.${base}/auto_net_config/auto_net_config"))
      .orElse(fetchAnc(s"http://${base}/auto_net_config/auto_net_config"))
      .orElse(fetchAnc(s"http://auto_net_config.${base}/auto_net_config/auto_net_config"))

    info(s"Result of fetching ANC from ${base}: ${res}")
    
    // if (res.isDefined) {
    //   val text = 
    //   IO.write(Convert.jsonToText(Convert.classToJson(res.get)), path, true)
    // }
    res
  }
}