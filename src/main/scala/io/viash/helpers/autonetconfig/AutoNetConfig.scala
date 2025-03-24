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

case class AutoNetConfig(
  api_version: String,
  valid_until: String,
  provided_by: String,
  name: String,
  prefix: String,
  hosts: Hosts,
  validation: Map[String, Map[String, Validation]]
)

object AutoNetConfig extends Logging {
  def fetchAnc(uri: String): Option[(AutoNetConfig, String)] = {
    info(s"Fetching ANC from ${uri}")
    val txt = IO.readSome(URI(uri))
    if (txt.isEmpty) return None

    txt match {
      case None => None
      case Some(s) => {
        val json = Convert.textToJson(s, uri)
        val anc = Convert.jsonToClass[AutoNetConfig](json, uri)    
        Some((anc, s))
      }
    }
  }

  def fetch(base: String): Option[AutoNetConfig] = {

    // check if found in cache and not expired
    // if not, fetch from the network
    val cachePath = Paths.get(SysEnv.viashHome).resolve("anc-cache").resolve(base)
    if (cachePath.toFile().exists()) {
      info(s"Found ANC cache at ${cachePath}")
      // read file and convert to AutoNetConfig
      val txt = IO.readSome(cachePath.toUri())
      val anc = txt.map { txt =>
        val json = Convert.textToJson(txt, cachePath.toUri().toString())
        Convert.jsonToClass[AutoNetConfig](json, cachePath.toUri().toString())
      }
      anc match {
        case None => info(s"Failed to parse ANC from ${base}")
        case Some(anc) => {
          val now = java.time.Instant.now()
          info(s"Now: ${now}")
          val validUntil = java.time.Instant.parse(anc.valid_until)
          info(s"Valid until: ${validUntil}")
          if (validUntil.isBefore(now)) {
            info(s"ANC from ${base} is expired")
          } else {
            info(s"Returning cached ANC from ${base}")
            return Some(anc)
          }
        }
      }
    }
 
    val res = fetchAnc(s"https://${base}/auto-net-config/auto-net-config")
      .orElse(fetchAnc(s"https://auto-net-config.${base}/auto-net-config/auto-net-config"))
      .orElse(fetchAnc(s"http://${base}/auto-net-config/auto-net-config"))
      .orElse(fetchAnc(s"http://auto-net-config.${base}/auto-net-config/auto-net-config"))

    info(s"Result of fetching ANC from ${base}: ${res}")
    
    if (res.isDefined) {
      cachePath.getParent().toFile().mkdirs()
      IO.write(res.get._2, cachePath, overwrite = true)
    }
    res.map(_._1)
  }
}