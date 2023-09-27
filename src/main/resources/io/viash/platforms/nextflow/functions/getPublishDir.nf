def getPublishDir() {
  return params.containsKey("publish_dir") ? params.publish_dir : 
    params.containsKey("publishDir") ? params.publishDir : 
    null
}
