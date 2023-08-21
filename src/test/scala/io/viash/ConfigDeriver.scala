package io.viash

import java.nio.file.{Path, Paths, Files}
import io.circe.yaml.parser
import io.circe.yaml.{Printer => YamlPrinter}
import io.viash.helpers.IO
import io.viash.config_mods.ConfigMods

final case class ConfigDeriver(
  baseConfigFile: Path,
  targetFolder: Path
) {

  // Prepare ConfigDeriver by copying base resources to the targetFolder
  {
    val rootPath = baseConfigFile.getParent
    IO.copyFolder(rootPath, targetFolder)
  }

  def derive(configMod: String, name: String): String = {
    ConfigDeriver.deriveNewConfig(baseConfigFile, targetFolder, configMod, name)
  }

  def derive(configMods: List[String], name: String): String = {
    ConfigDeriver.deriveNewConfig(baseConfigFile, targetFolder, configMods, name)
  }
}

object ConfigDeriver {


  def deriveNewConfig(baseConfig: Path, targetFolder: Path, configMod: String, name: String): String = {
    deriveNewConfig(baseConfig, targetFolder, List(configMod), name)
  }

  /**
    * Derive a config file from the default config
    * @param comnifMods Config mods to apply to the yaml
    * @param name name of the new .vsh.yaml file
    * @return Path to the new config file as string
    */
  def deriveNewConfig(baseConfig: Path, targetFolder: Path, configMods: List[String], name: String): String = {
    val shFileStr = s"$name.sh"
    val yamlFileStr = s"$name.vsh.yaml"
    val newConfigFilePath = Paths.get(targetFolder.toString, s"$yamlFileStr")
    
    val yamlText = IO.read(IO.uri(baseConfig.toString))

    def errorHandler[C](e: Exception): C = {
      Console.err.println(s"${Console.RED}Error parsing '$name'.${Console.RESET}\nDetails:")
      throw e
    }

    val js = parser.parse(yamlText).fold(errorHandler, a => a)

    val confMods = ConfigMods.parseConfigMods(configMods)

    val modifiedJs = confMods(js, preparse = false)

    val yamlPrinter = YamlPrinter(
      preserveOrder = true,
      dropNullKeys = true,
      mappingStyle = YamlPrinter.FlowStyle.Block,
      splitLines = true,
      stringStyle = YamlPrinter.StringStyle.Plain
    )
    val yaml = yamlPrinter.pretty(modifiedJs)
    Files.write(newConfigFilePath, yaml.getBytes)

    newConfigFilePath.toString
  }
}
