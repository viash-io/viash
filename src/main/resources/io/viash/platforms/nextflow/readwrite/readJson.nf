def readJson(file_path) {
  def inputFile = file_path !instanceof Path ? file(file_path, hidden: true) : file_path
  def jsonSlurper = new groovy.json.JsonSlurper()
  jsonSlurper.parse(inputFile)
}
