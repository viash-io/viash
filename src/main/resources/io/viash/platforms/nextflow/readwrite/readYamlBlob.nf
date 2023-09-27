def readYamlBlob(str) {
  def yamlSlurper = new org.yaml.snakeyaml.Yaml()
  yamlSlurper.load(str)
}
