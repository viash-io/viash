package com.dataintuitive.viash.config

case class Info(
  functionality_path: Option[String] = None,
  platform_path: Option[String] = None,
  platform_id: Option[String] = None,
  joined_path: Option[String] = None,
  output_path: Option[String] = None,
  executable_path: Option[String] = None,
  viash_version: Option[String] = None,
  git_commit: Option[String] = None,
  git_remote: Option[String] = None
) {
  def consoleString = {
    s"""viash version:      ${viash_version.getOrElse("NA")}
       |functionality path: ${functionality_path.getOrElse("NA")}
       |platform path:      ${platform_path.getOrElse("NA")}
       |platform id:        ${platform_id.getOrElse("NA")}
       |joined path:        ${joined_path.getOrElse("NA")}
       |executable path:    ${executable_path.getOrElse("NA")}
       |output path:        ${output_path.getOrElse("NA")}
       |remote git repo:    ${git_remote.getOrElse("NA")}""".stripMargin
  }

  def parent_path = {
    val regex = "[^/]*$".r
    if (functionality_path.isDefined) {
      Some(regex.replaceFirstIn(functionality_path.get, ""))
    } else if (joined_path.isDefined) {
      Some(regex.replaceFirstIn(joined_path.get, ""))
    } else {
      None
    }
  }
}