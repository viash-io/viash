def readJsonBlob(str) {
  def jsonSlurper = new groovy.json.JsonSlurper()
  jsonSlurper.parseText(str)
}
