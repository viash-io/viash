String toJsonBlob(Map data) {
  return groovy.json.JsonOutput.toJson(data)
}
