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
    tuple val(id), val(yamlBlob), val(yamlFile), path(inputFiles, stageAs: "_inputfile?/*"), val(outputFiles)
  output:
    tuple val(id), path{[yamlFile] + outputFiles}
  script:
  def copyCommands = [
    inputFiles instanceof List ? inputFiles : [inputFiles],
    outputFiles instanceof List ? outputFiles : [outputFiles]
  ]
    .transpose()
    .collectMany{infile, outfile ->
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


// this assumes that the state contains no other values other than those specified in the config
def publishStatesByConfig(Map args) {
  def key_ = args.get("key")
  def config = args.get("config")
  
  workflow publishStatesSimpleWf {
    take: input_ch
    main:
      input_ch
        | map { tup ->
          def id_ = tup[0]
          def state_ = tup[1] // e.g. [output: new File("myoutput.h5ad"), k: 10]
          def origState_ = tup[2] // e.g. [output: '$id.$key.foo.h5ad']

          // the processed state is a list of [key, value, srcPath, destPath] tuples, where
          //   - key, value is part of the state to be saved to disk
          //   - srcPath and destPath are lists of files to be copied from src to dest
          def processedState =
            config.functionality.allArguments
              .findAll { it.direction == "output" }
              .collectMany { par ->
                def plainName_ = par.plainName
                // if the state does not contain the key, it's an
                // optional argument for which the component did 
                // not generate any output
                if (!state_.containsKey(plainName_)) {
                  return []
                }
                def value = state_[plainName_]
                // if the parameter is not a file, it should be stored
                // in the state as-is, but is not something that needs 
                // to be copied from the source path to the dest path
                if (par.type != "file") {
                  return [[key: plainName_, value: value, srcPath: [], destPath: []]]
                }
                // if the orig state does not contain this filename,
                // it's an optional argument for which the user specified
                // that it should not be returned as a state
                if (!origState_.containsKey(plainName_)) {
                  return []
                }
                def filenameTemplate = origState_[plainName_]
                // if the pararameter is multiple: true, fetch the template
                if (par.multiple && filenameTemplate instanceof List) {
                  filenameTemplate = filenameTemplate[0]
                }
                // instantiate the template
                filename = filenameTemplate
                  .replaceAll('\\$id', id_)
                  .replaceAll('\\$key', key_)
                if (par.multiple) {
                  // if the parameter is multiple: true, the filename
                  // should contain a wildcard '*' that is replaced with
                  // the index of the file
                  assert filename.contains("*") : "Module '${key_}' id '${id_}': Multiple output files specified, but no wildcard '*' in the filename: ${filename}"
                  def outputPerFile = value.withIndex().collect{ val, ix ->
                    def destPath = filename.replace("*", ix.toString())
                    def destFile = new File(destPath)
                    def srcPath = val instanceof File ? val.toPath() : val
                    [value: destFile, srcPath: srcPath, destPath: destPath]
                  }
                  def transposedOutputs = ["value", "srcPath", "destPath"].collectEntries{ key -> 
                    [key, outputPerFile.collect{dic -> dic[key]}]
                  }
                  return [[key: plainName_] + transposedOutputs]
                } else {
                  def destFile = new File(filename)
                  def srcPath = value instanceof File ? value.toPath() : value
                  return [[key: plainName_, value: destFile, srcPath: [srcPath], destPath: [filename]]]
                }
              }
          
          def updatedState_ = processedState.collectEntries{[it.key, it.value]}
          def inputFiles_ = processedState.collectMany{it.srcPath}
          def outputFiles_ = processedState.collectMany{it.destPath}
          
          // convert state to yaml blob
          def yamlBlob_ = toTaggedYamlBlob([id: id_] + updatedState_)

          // adds a leading dot to the id (after any folder names)
          // example: foo -> .foo, foo/bar -> foo/.bar
          // TODO: allow defining the state.yaml template
          def idWithDot_ = id_.replaceAll("^(.+/)?([^/]+)", "\$1.\$2")
          def yamlFile = '$id.$key.state.yaml'
            .replaceAll('\\$id', idWithDot_)
            .replaceAll('\\$key', key_)

          [id_, yamlBlob_, yamlFile, inputFiles_, outputFiles_]
        }
        | publishStatesProc
    emit: input_ch
  }
  return publishStatesSimpleWf
}