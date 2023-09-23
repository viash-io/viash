process publishStatesProc {
  // todo: check publishpath?
  publishDir path: "${getPublishDir()}/${id}/", mode: "copy"
  tag "$id"
  input:
    tuple val(id), val(yamlBlob), path(inputFiles)
  output:
    tuple val(id), path{["state.yaml"] + inputFiles}
  script:
  """
  echo '${yamlBlob}' > state.yaml
  """
}

def collectFiles(obj) {
  if (obj instanceof java.io.File || obj instanceof Path)  {
    return [obj]
  } else if (obj instanceof List && obj !instanceof String) {
    return obj.collectMany{item ->
      collectFiles(item)
    }
  } else if (obj instanceof Map) {
    return obj.collectMany{key, item ->
      collectFiles(item)
    }
  } else {
    return []
  }
}


def iterateMap(obj, fun) {
  if (obj instanceof List && obj !instanceof String) {
    return obj.collect{item ->
      iterateMap(item, fun)
    }
  } else if (obj instanceof Map) {
    return obj.collectEntries{key, item ->
      [key.toString(), iterateMap(item, fun)]
    }
  } else {
    return fun(obj)
  }
}

def convertPathsToFile(obj) {
  iterateMap(obj, {x ->
    if (x instanceof File) {
      return x
    } else if (x instanceof Path)  {
      return x.toFile()
    } else {
      return x
    }
  })
}
def convertFilesToPath(obj) {
  iterateMap(obj, {x ->
    if (x instanceof Path) {
      return x
    } else if (x instanceof File)  {
      return x.toPath()
    } else {
      return x
    }
  })
}

def publishStates(Map args) {
  workflow publishStatesWf {
    take: input_ch
    main:
      input_ch
        | map { tup ->
          def id = tup[0]
          def state = tup[1]
          def files = collectFiles(state)
          def convertedState = [id: id] + convertPathsToFile(state)
          def yamlBlob = toTaggedYamlBlob(convertedState)
          [id, yamlBlob, files]
        }
        | publishStatesProc
    emit: input_ch
  }
  return publishStatesWf
}
