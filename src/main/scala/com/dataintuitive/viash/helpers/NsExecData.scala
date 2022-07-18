package com.dataintuitive.viash.helpers

import io.viash.config.Config
import java.nio.file.Paths

final case class NsExecData(
  configFullPath: String,
  absoluteConfigFullPath: String,
  dir: String,
  absoluteDir: String,
  mainScript: String,
  absoluteMainScript: String,
  functionalityName: String
)

object NsExecData {
  def apply(configPath: String, config: Config):NsExecData = {
    val configPath_ = Paths.get(configPath)
    val mainScript = config.functionality.mainScript.flatMap(s => s.path)
    val absoluteMainScript = mainScript.flatMap(m => Some(Paths.get(m).toAbsolutePath.toString))
    NsExecData(
      configFullPath = configPath,
      absoluteConfigFullPath = configPath_.toAbsolutePath.toString,
      dir = configPath_.getParent.toString,
      absoluteDir = configPath_.toAbsolutePath.getParent.toString,
      mainScript = mainScript.getOrElse(""),
      absoluteMainScript = absoluteMainScript.getOrElse(""),
      functionalityName = config.functionality.name
    )
  }
}
