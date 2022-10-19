package io.viash.lenses

import monocle.PLens
import io.viash.functionality.dependencies.Repository

object RepositoryLens {
  val nameLens = PLens[Repository, Repository, String, String](r => r.name)(s => r => r.copyRepo(name = s))
  val typeLens = PLens[Repository, Repository, String, String](r => r.`type`)(s => r => r.copyRepo(`type` = s))
  val tagLens = PLens[Repository, Repository, Option[String], Option[String]](r => r.tag)(s => r => r.copyRepo(tag = s))
  val pathLens = PLens[Repository, Repository, Option[String], Option[String]](r => r.path)(s => r => r.copyRepo(path = s))
  val localPathLens = PLens[Repository, Repository, String, String](r => r.localPath)(s => r => r.copyRepo(localPath = s))
}
