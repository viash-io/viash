def readYaml(file_path) {
  def inputFile = file_path !instanceof Path ? file(file_path, hidden: true) : file_path
  def yamlSlurper = new org.yaml.snakeyaml.Yaml()
  yamlSlurper.load(inputFile)
}
