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

package io.viash.platforms.requirements

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
case class RRequirements(
  @description("Specifies which packages to install from CRAN.")
  @example("packages: [ anndata, ggplot2 ]", "yaml")
  packages: OneOrMore[String] = Zero,

  @description("Specifies which packages to install from CRAN.")
  @example("cran: [ anndata, ggplot2 ]", "yaml")
  cran: OneOrMore[String] = Zero,

  @description("Specifies which packages to install from BioConductor.")
  @example("bioc: [ AnnotationDbi ]", "yaml")
  bioc: OneOrMore[String] = Zero,
  
  @description("Specifies which packages to install using a Git URI.")
  @example("git: [ https://some.git.repository/org/repo ]", "yaml")
  git: OneOrMore[String] = Zero,

  @description("Specifies which packages to install from GitHub.")
  @example("github: [ rcannood/SCORPIUS ]", "yaml")
  github: OneOrMore[String] = Zero,

  @description("Specifies which packages to install from GitLab.")
  @example("gitlab: [ org/package ]", "yaml")
  gitlab: OneOrMore[String] = Zero,

  @description("Specifies which packages to install from Bitbucket.")
  @example("bitbucket: [ org/package ]", "yaml")
  bitbucket: OneOrMore[String] = Zero,

  @description("Specifies which packages to install using an SVN URI.")
  @example("svn: [ https://path.to.svn/group/repo ]", "yaml")
  svn: OneOrMore[String] = Zero,

  @description("Specifies which packages to install using a generic URI.")
  @example("url: [ https://github.com/hadley/stringr/archive/HEAD.zip ]", "yaml")
  url: OneOrMore[String] = Zero,

  @description("Specifies a code block to run as part of the build.")
  @example("""script: |
    #  cat("Running custom code\n")
    #  install.packages("anndata")""".stripMargin('#'), "yaml")
  script: OneOrMore[String] = Zero,
  
  @description("Forces packages specified in `bioc` to be reinstalled, even if they are already present in the container. Default: false.")
  @example("bioc_force_install: false", "yaml")
  bioc_force_install: Boolean = false,
  
  `type`: String = "r"
) extends Requirements {assert(script.forall(!_.contains("'")))

  def installCommands: List[String] = {
    val installRemotes =
      if ((packages ::: cran ::: git ::: github ::: gitlab ::: bitbucket ::: svn ::: url).nonEmpty) {
        List("""Rscript -e 'if (!requireNamespace("remotes", quietly = TRUE)) install.packages("remotes")'""")
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
        List("""Rscript -e 'if (!requireNamespace("BiocManager", quietly = TRUE)) install.packages("BiocManager")'""")
      } else {
        Nil
      }
    val installBioc =
      if (bioc.nonEmpty) {
        if (bioc_force_install) {
          List(s"""Rscript -e 'BiocManager::install(c("${bioc.mkString("\", \"")}"))'""")
        } else {
          bioc.map { biocPackage =>
            s"""Rscript -e 'if (!requireNamespace("$biocPackage", quietly = TRUE)) BiocManager::install("$biocPackage")'"""
          }
        }
      } else {
        Nil
      }

    val installers = remotePairs.flatMap {
      case (_, Nil) => None
      case (str, list) =>
        Some(s"""Rscript -e 'remotes::install_$str(c("${list.mkString("\", \"")}"), repos = "https://cran.rstudio.com")'""")
    }

    val installScript =
      if (script.nonEmpty) {
        script.map { line =>
          s"""Rscript -e '$line'"""
        }
      } else {
        Nil
      }

    installRemotes ::: installBiocManager ::: installBioc ::: installers ::: installScript
  }
}
