package com.dataintuitive.viash.config

case class Info(
  functionality_path: Option[String],
  platform_path: Option[String],
  platform_id: Option[String],
  executable_path: Option[String],
  output_path: Option[String],
  viash_version: Option[String],
  git_commit: Option[String],
  git_remote: Option[String]
) {
  def consoleString =
    s"""viash version:      ${viash_version.getOrElse("NA")}
       |functionality path: ${functionality_path.getOrElse("NA")}
       |platform path:      ${platform_path.getOrElse("NA")}
       |platform id:        ${platform_id.getOrElse("NA")}
       |executable path:    ${executable_path.getOrElse("NA")}
       |output path:        ${output_path.getOrElse("NA")}
       |remote git repo:    ${git_remote.getOrElse("NA")}""".stripMargin
}