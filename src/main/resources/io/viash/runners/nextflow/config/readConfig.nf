
def readConfig(file) {
  def config = readYaml(file ?: moduleDir.resolve("config.vsh.yaml"))
  processConfig(config)
}
