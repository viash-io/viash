package com.dataintuitive.viash.platforms.requirements

case class RRequirements(
  packages: List[String] = Nil,
  cran: List[String] = Nil,
  bioc: List[String] = Nil,
  git: List[String] = Nil,
  github: List[String] = Nil,
  gitlab: List[String] = Nil,
  bitbucket: List[String] = Nil,
  svn: List[String] = Nil,
  url: List[String] = Nil,
  bioc_force_install: Boolean = false
) extends Requirements {
  val `type` = "r"

  def installCommands: List[String] = {
    val installRemotes =
      if ((cran ::: git ::: github ::: gitlab ::: bitbucket ::: svn ::: url).nonEmpty) {
        List("""Rscript -e 'if (!requireNamespace("remotes", quietly = TRUE)) install.packages("remotes")'""")
      } else {
        Nil
      }

    val remotePairs = List(
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

    installRemotes ::: installBiocManager ::: installBioc ::: installers
  }
}
