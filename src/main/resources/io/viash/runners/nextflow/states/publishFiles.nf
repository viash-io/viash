def publishFiles(Map args) {
  def key_ = args.get("key")

  assert key_ != null : "publishFiles: key must be specified"
  
  workflow publishFilesWf {
    take: input_ch
    main:
      input_ch
        | map { tup ->
          def id_ = tup[0]
          def state_ = tup[1]

          // the input files and the target output filenames
          def inputoutputFilenames_ = collectInputOutputPaths(state_, id_ + "." + key_).transpose()
          def inputFiles_ = inputoutputFilenames_[0]
          def outputFilenames_ = inputoutputFilenames_[1]

          [id_, inputFiles_, outputFilenames_]
        }
        | publishFilesProc
    emit: input_ch
  }
  return publishFilesWf
}

process publishFilesProc {
  // todo: check publishpath?
  publishDir path: "${getPublishDir()}/", mode: "copy"
  tag "$id"
  input:
    tuple val(id), path(inputFiles, stageAs: "_inputfile?/*"), val(outputFiles)
  output:
    tuple val(id), path{outputFiles}
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
  echo "Copying output files to destination folder"
  ${copyCommands.join("\n  ")}
  """
}


// this assumes that the state contains no other values other than those specified in the config
def publishFilesByConfig(Map args) {
  def config = args.get("config")
  assert config != null : "publishFilesByConfig: config must be specified"

  def key_ = args.get("key", config.name)
  assert key_ != null : "publishFilesByConfig: key must be specified"
  
  workflow publishFilesSimpleWf {
    take: input_ch
    main:
      input_ch
        | map { tup ->
          def id_ = tup[0]
          def state_ = tup[1] // e.g. [output: new File("myoutput.h5ad"), k: 10]
          def origState_ = tup[2] // e.g. [output: '$id.$key.foo.h5ad']


          // the processed state is a list of [key, value, inputPath, outputFilename] tuples, where
          //   - key is a String
          //   - value is any object that can be serialized to a Yaml (so a String/Integer/Long/Double/Boolean, a List, a Map, or a Path)
          //   - inputPath is a List[Path]
          //   - outputFilename is a List[String]
          //   - (inputPath, outputFilename) are the files that will be copied from src to dest (relative to the state.yaml)
          def processedState =
            config.allArguments
              .findAll { it.direction == "output" }
              .collectMany { par ->
                def plainName_ = par.plainName
                // if the state does not contain the key, it's an
                // optional argument for which the component did 
                // not generate any output OR multiple channels were emitted
                // and the output was just not added to using the channel
                // that is now being parsed
                if (!state_.containsKey(plainName_)) {
                  return []
                }
                def value = state_[plainName_]
                // if the parameter is not a file, it should be stored
                // in the state as-is, but is not something that needs 
                // to be copied from the source path to the dest path
                if (par.type != "file") {
                  return [[inputPath: [], outputFilename: []]]
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
                  .replaceAll('\\$\\{id\\}', id_)
                  .replaceAll('\\$key', key_)
                  .replaceAll('\\$\\{key\\}', key_)
                if (par.multiple) {
                  // if the parameter is multiple: true, the filename
                  // should contain a wildcard '*' that is replaced with
                  // the index of the file
                  assert filename.contains("*") : "Module '${key_}' id '${id_}': Multiple output files specified, but no wildcard '*' in the filename: ${filename}"
                  def outputPerFile = value.withIndex().collect{ val, ix ->
                    def filename_ix = filename.replace("*", ix.toString())
                    def inputPath = val instanceof File ? val.toPath() : val
                    [inputPath: inputPath, outputFilename: filename_ix]
                  }
                  def transposedOutputs = ["inputPath", "outputFilename"].collectEntries{ key -> 
                    [key, outputPerFile.collect{dic -> dic[key]}]
                  }
                  return [[key: plainName_] + transposedOutputs]
                } else {
                  def value_ = java.nio.file.Paths.get(filename)
                  def inputPath = value instanceof File ? value.toPath() : value
                  return [[inputPath: [inputPath], outputFilename: [filename]]]
                }
              }
          
          def inputPaths = processedState.collectMany{it.inputPath}
          def outputFilenames = processedState.collectMany{it.outputFilename}
          

          [id_, inputPaths, outputFilenames]
        }
        | publishFilesProc
    emit: input_ch
  }
  return publishFilesSimpleWf
}



