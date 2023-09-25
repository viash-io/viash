String toYamlBlob(Map data) {
  def options = new org.yaml.snakeyaml.DumperOptions()
  options.setDefaultFlowStyle(org.yaml.snakeyaml.DumperOptions.FlowStyle.BLOCK)
  options.setPrettyFlow(true)
  def yaml = new org.yaml.snakeyaml.Yaml(options)
  def cleanData = iterateMap(data, {it.toString()})
  return yaml.dump(data)
}
