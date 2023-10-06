String toYamlBlob(data) {
  def options = new org.yaml.snakeyaml.DumperOptions()
  options.setDefaultFlowStyle(org.yaml.snakeyaml.DumperOptions.FlowStyle.BLOCK)
  options.setPrettyFlow(true)
  def yaml = new org.yaml.snakeyaml.Yaml(options)
  def cleanData = iterateMap(data, { it instanceof Path ? it.toString() : it })
  return yaml.dump(cleanData)
}
