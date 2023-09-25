process publishStatesProc {
  // todo: check publishpath?
  publishDir path: "${getPublishDir()}/${id}/", mode: "copy"
  tag "$id"
  input:
    tuple val(id), val(yamlFile), val(yamlBlob), path(inputFiles)
  output:
    tuple val(id), path{[yamlFile] + inputFiles}
  script:
  """
  mkdir -p "\$(dirname '${yamlFile}')"
  echo '${yamlBlob}' > '${yamlFile}'
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
  def key_ = args.get("key")
  
  workflow publishStatesWf {
    take: input_ch
    main:
      input_ch
        | map { tup ->
          def id_ = tup[0]
          def state_ = tup[1]
          def files_ = collectFiles(state_)
          def convertedState_ = [id: id_] + convertPathsToFile(state_)
          def yamlBlob_ = toTaggedYamlBlob(convertedState_)
          // adds a leading dot
          // example: foo -> .foo, foo/bar -> foo/.bar
          def idWithDot_ = id_.replaceAll("^(.+/)?([^/]+)", "\$1.\$2")
          def yamlFile = '$id.$key.state.yaml'
            .replaceAll('\\$id', id_)
            .replaceAll('\\$key', key_)

          [id_, yamlFile, yamlBlob_, files_]
        }
        | publishStatesProc
    emit: input_ch
  }
  return publishStatesWf
}
