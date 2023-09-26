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

/**
 * Recurse through a state and collect all input files and their target output filenames.
 * @param obj The state to recurse through.
 * @param prefix The prefix to prepend to the output filenames.
 */
def collectInputOutputPaths(obj, prefix) {
  if (obj instanceof java.io.File || obj instanceof Path)  {
    def file = obj instanceof File ? obj : obj.toFile()
    def ext = file.name.find("\\.[^\\.]+\$") ?: ""
    def newFilename = prefix + ext
    return [[obj, newFilename]]
  } else if (obj instanceof List && obj !instanceof String) {
    return obj.withIndex().collectMany{item, ix ->
      collectInputOutputPaths(item, prefix + "_" + ix)
    }
  } else if (obj instanceof Map) {
    return obj.collectMany{key, item ->
      collectInputOutputPaths(item, prefix + "." + key)
    }
  } else {
    return []
  }
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

          // the input files and the target output filenames
          def inputOutputFiles_ = collectInputOutputPaths(state_, id_ + "." + key_).transpose()
          def inputFiles_ = inputOutputFiles_[0]
          def outputFiles_ = inputOutputFiles_[1]

          // convert all paths to files before converting it to a yaml blob
          def convertedState_ = [id: id_] + convertPathsToFile(state_)

          // convert state to yaml blob
          def yamlBlob_ = toTaggedYamlBlob(convertedState_)

          // adds a leading dot to the id (after any folder names)
          // example: foo -> .foo, foo/bar -> foo/.bar
          def idWithDot_ = id_.replaceAll("^(.+/)?([^/]+)", "\$1.\$2")
          def yamlFile = '$id.$key.state.yaml'
            .replaceAll('\\$id', idWithDot_)
            .replaceAll('\\$key', key_)

          [id_, yamlBlob_, yamlFile, inputFiles_, outputFiles_]
        }
        | publishStatesProc
    emit: input_ch
  }
  return publishStatesWf
}
process publishStatesProc {
  // todo: check publishpath?
  publishDir path: "${getPublishDir()}/", mode: "copy"
  tag "$id"
  input:
    tuple val(id), val(yamlBlob), val(yamlFile), path(inputFiles), val(outputFiles)
  output:
    tuple val(id), path{[yamlFile] + outputFiles}
  script:
  def copyCommands = [
    inputFiles instanceof List ? inputFiles : [inputFiles],
    outputFiles instanceof List ? outputFiles : [outputFiles]
  ].transpose().collectMany{infile, outfile ->
    if (infile.toString() != outfile.toString()) {
      ["cp -r '${infile.toString()}' '${outfile.toString()}'"]
    } else {
      // no need to copy if infile is the same as outfile
      []
    }
  }
  """
  mkdir -p "\$(dirname '${yamlFile}')"
  echo "Storing state as yaml"
  echo '${yamlBlob}' > '${yamlFile}'
  echo "Copying output files to destination folder"
  ${copyCommands.join("\n  ")}
  """
}
