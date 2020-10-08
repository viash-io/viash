package com.dataintuitive.viash.config

case class Info(
  config: String,
  platform: Option[String] = None,
  output: Option[String] = None,
  executable: Option[String] = None,
  viash_version: Option[String] = None,
  git_commit: Option[String] = None,
  git_remote: Option[String] = None
) {
  def consoleString: String = {
    val missing = "<NA>"
    s"""viash version:      ${viash_version.getOrElse(missing)}
       |config:             ${config}
       |platform:           ${platform.getOrElse(missing)}
       |executable:         ${executable.getOrElse(missing)}
       |output:             ${output.getOrElse(missing)}
       |remote git repo:    ${git_remote.getOrElse(missing)}""".stripMargin
  }

  def parent_path: String = {
    val regex = "[^/]*$".r
    regex.replaceFirstIn(config, "")
  }
}