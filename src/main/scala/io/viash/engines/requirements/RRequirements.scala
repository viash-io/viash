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

package io.viash.engines.requirements

import io.viash.helpers.data_structures._
import io.viash.schemas._

@description("Specify which R packages should be available in order to run the component.")
@example(
  """setup: 
    |  - type: r
    |    cran: anndata
    |    bioc: [ AnnotationDbi, SingleCellExperiment ]
    |    github: rcannood/SCORPIUS
    |""".stripMargin,
    "yaml")
@subclass("r")
case class RRequirements(
  @description("Specifies which packages to install from CRAN.")
  @example("packages: [ anndata, ggplot2 ]", "yaml")
  @default("Empty")
  packages: OneOrMore[String] = Nil,

  @description("Specifies which packages to install from CRAN.")
  @example("cran: [ anndata, ggplot2 ]", "yaml")
  @default("Empty")
  cran: OneOrMore[String] = Nil,

  @description("Specifies which packages to install from BioConductor.")
  @example("bioc: [ AnnotationDbi ]", "yaml")
  @default("Empty")
  bioc: OneOrMore[String] = Nil,
  
  @description("Specifies which packages to install using a Git URI.")
  @example("git: [ https://some.git.repository/org/repo ]", "yaml")
  @default("Empty")
  git: OneOrMore[String] = Nil,

  @description("Specifies which packages to install from GitHub.")
  @example("github: [ rcannood/SCORPIUS ]", "yaml")
  @default("Empty")
  github: OneOrMore[String] = Nil,

  @description("Specifies which packages to install from GitLab.")
  @example("gitlab: [ org/package ]", "yaml")
  @default("Empty")
  gitlab: OneOrMore[String] = Nil,

  @description("Specifies which packages to install from Bitbucket.")
  @example("bitbucket: [ org/package ]", "yaml")
  @default("Empty")
  bitbucket: OneOrMore[String] = Nil,

  @description("Specifies which packages to install using an SVN URI.")
  @example("svn: [ https://path.to.svn/group/repo ]", "yaml")
  @default("Empty")
  svn: OneOrMore[String] = Nil,

  @description("Specifies which packages to install using a generic URI.")
  @example("url: [ https://github.com/hadley/stringr/archive/HEAD.zip ]", "yaml")
  @default("Empty")
  url: OneOrMore[String] = Nil,

  @description("Specifies a code block to run as part of the build.")
  @example("""script: |
    #  cat("Running custom code\n")
    #  install.packages("anndata")""".stripMargin('#'), "yaml")
  @default("Empty")
  script: OneOrMore[String] = Nil,
  
  @description("Forces packages specified in `bioc` to be reinstalled, even if they are already present in the container. Default: false.")
  @example("bioc_force_install: false", "yaml")
  @default("False")
  bioc_force_install: Boolean = false,

  @description("Specifies whether to treat warnings as errors. Default: true.")
  @example("warnings_as_errors: true", "yaml")
  @default("True")
  warnings_as_errors: Boolean = true,
  
  `type`: String = "r"
) extends Requirements {
  def installCommands: List[String] = {
    val prefix = if (warnings_as_errors) "options(warn = 2); " else ""

    def runRCode(code: String): String = {
      s"""Rscript -e '${prefix}${code.replaceAll("'", "'\"'\"'")}'"""
    }

    val installRemotes =
      if ((packages ::: cran ::: git ::: github ::: gitlab ::: bitbucket ::: svn ::: url).nonEmpty) {
        List(runRCode("""if (!requireNamespace("remotes", quietly = TRUE)) install.packages("remotes")"""))
      } else {
        Nil
      }

    val remotePairs: List[(String, List[String])] = List(
      ("cran", cran ::: packages),
      ("git", git),
      ("github", github),
      ("gitlab", gitlab),
      ("bitbucket", bitbucket),
      ("svn", svn),
      ("url", url)
    )

    val installBiocManager =
      if (bioc.nonEmpty) {
        List(runRCode("""if (!requireNamespace("BiocManager", quietly = TRUE)) install.packages("BiocManager")"""))
      } else {
        Nil
      }
    val installBioc =
      if (bioc.nonEmpty) {
        if (bioc_force_install) {
          List(runRCode(s"""BiocManager::install(c("${bioc.mkString("\", \"")}"))"""))
        } else {
          bioc.map { biocPackage =>
            runRCode(s"""if (!requireNamespace("$biocPackage", quietly = TRUE)) BiocManager::install("$biocPackage")""")
          }
        }
      } else {
        Nil
      }

    val installers = remotePairs.flatMap {
      case (_, Nil) => None
      case (str, list) =>
        Some(runRCode(s"""remotes::install_$str(c("${list.mkString("\", \"")}"), repos = "https://cran.rstudio.com")"""))
    }

    val installScript =
      if (script.nonEmpty) {
        script.map { line =>
          runRCode(line)
        }
      } else {
        Nil
      }

    installRemotes ::: installBiocManager ::: installBioc ::: installers ::: installScript
  }
}
