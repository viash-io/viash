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

package io.viash.lenses

import monocle.PLens
import io.viash.config.dependencies.Package
import io.viash.config.dependencies.PackageWithName

object PackageLens {
  // val nameLens = PLens[PackageWithName, PackageWithName, String, String](r => r.name)(s => r => r.copyPackage(name = s))
  val typeLens = PLens[Package, Package, String, String](r => r.`type`)(s => r => r.copyPackage(`type` = s))
  val tagLens = PLens[Package, Package, Option[String], Option[String]](r => r.tag)(s => r => r.copyPackage(tag = s))
  val pathLens = PLens[Package, Package, Option[String], Option[String]](r => r.path)(s => r => r.copyPackage(path = s))
  val localPathLens = PLens[Package, Package, String, String](r => r.localPath)(s => r => r.copyPackage(localPath = s))
}
