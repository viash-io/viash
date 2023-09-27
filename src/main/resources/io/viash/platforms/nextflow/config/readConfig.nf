
def readConfig(file) {
  def config = readYaml(file != null ? file : "$projectDir/config.vsh.yaml")
  processConfig(config)
}
