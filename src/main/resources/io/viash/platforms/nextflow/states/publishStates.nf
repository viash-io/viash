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

/**
 * Recurse through a state and collect all input files and their target output filenames.
 * @param obj The state to recurse through.
 * @param prefix The prefix to prepend to the output filenames.
 */
def collectInputOutputPaths(obj, prefix) {
  if (obj instanceof File || obj instanceof Path)  {
    def path = obj instanceof Path ? obj : obj.toPath()
    def ext = path.getFileName().toString().find("\\.[^\\.]+\$") ?: ""
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
  def yamlTemplate_ = args.get("output_state", args.get("outputState", '$id.$key.state.yaml'))

  assert key_ != null : "publishStates: key must be specified"
  
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

          def yamlFilename = yamlTemplate_
            .replaceAll('\\$id', id_)
            .replaceAll('\\$key', key_)

            // TODO: do the pathnames in state_ match up with the outputFiles_?

          // convert state to yaml blob
          def yamlBlob_ = toRelativeTaggedYamlBlob([id: id_] + state_, java.nio.file.Paths.get(yamlFilename))

          [id_, yamlBlob_, yamlFilename, inputFiles_, outputFiles_]
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
        [
          "[ -d \"\$(dirname '${outfile.toString()}')\" ] || mkdir -p \"\$(dirname '${outfile.toString()}')\"",
          "cp -r '${infile.toString()}' '${outfile.toString()}'"
        ]
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
  def config = args.get("config")
  assert config != null : "publishStatesByConfig: config must be specified"

  def key_ = args.get("key", config.functionality.name)
  assert key_ != null : "publishStatesByConfig: key must be specified"
  
  workflow publishStatesSimpleWf {
    take: input_ch
    main:
      input_ch
        | map { tup ->
          def id_ = tup[0]
          def state_ = tup[1] // e.g. [output: new File("myoutput.h5ad"), k: 10]
          def origState_ = tup[2] // e.g. [output: '$id.$key.foo.h5ad']

          // TODO: allow overriding the state.yaml template
          // TODO TODO: if auto.publish == "state", add output_state as an argument
          def yamlTemplate = params.containsKey("output_state") ? params.output_state : '$id.$key.state.yaml'
          def yamlFilename = yamlTemplate
            .replaceAll('\\$id', id_)
            .replaceAll('\\$key', key_)
          def yamlDir = java.nio.file.Paths.get(yamlFilename).getParent()

          // the processed state is a list of [key, value, srcPath, destPath] tuples, where
          //   - key is a String
          //   - value is any object that can be serialized to a Yaml (so a String/Integer/Long/Double/Boolean, a List, a Map, or a Path)
          //   - srcPath is a List[Path]
          //   - destPath is a List[String]
          //   - (key, value) are the tuples that will be saved to the state.yaml file
          //   - (srcPath, destPath) are the files that will be copied from src to dest (relative to the state.yaml)
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
                def filename = filenameTemplate
                  .replaceAll('\\$id', id_)
                  .replaceAll('\\$key', key_)
                if (par.multiple) {
                  // if the parameter is multiple: true, the filename
                  // should contain a wildcard '*' that is replaced with
                  // the index of the file
                  assert filename.contains("*") : "Module '${key_}' id '${id_}': Multiple output files specified, but no wildcard '*' in the filename: ${filename}"
                  def outputPerFile = value.withIndex().collect{ val, ix ->
                    def filename_ix = filename.replace("*", ix.toString())
                    def value_ = java.nio.file.Paths.get(filename_ix)
                    // if id contains a slash
                    if (yamlDir != null) {
                      value_ = yamlDir.relativize(value_)
                    }
                    def srcPath = val instanceof File ? val.toPath() : val
                    [value: value_, srcPath: srcPath, destPath: filename_ix]
                  }
                  def transposedOutputs = ["value", "srcPath", "destPath"].collectEntries{ key -> 
                    [key, outputPerFile.collect{dic -> dic[key]}]
                  }
                  return [[key: plainName_] + transposedOutputs]
                } else {
                  def value_ = java.nio.file.Paths.get(filename)
                  // if id contains a slash
                  if (yamlDir != null) {
                    value_ = yamlDir.relativize(value_)
                  }
                  def srcPath = value instanceof File ? value.toPath() : value
                  return [[key: plainName_, value: value_, srcPath: [srcPath], destPath: [filename]]]
                }
              }
          
          def updatedState_ = processedState.collectEntries{[it.key, it.value]}
          def inputFiles_ = processedState.collectMany{it.srcPath}
          def outputFiles_ = processedState.collectMany{it.destPath}
          
          // convert state to yaml blob
          def yamlBlob_ = toTaggedYamlBlob([id: id_] + updatedState_)

          [id_, yamlBlob_, yamlFilename, inputFiles_, outputFiles_]
        }
        | publishStatesProc
    emit: input_ch
  }
  return publishStatesSimpleWf
}
