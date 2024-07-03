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

          def yamlFilename = yamlTemplate_
            .replaceAll('\\$id', id_)
            .replaceAll('\\$key', key_)


          // convert state to yaml blob
          def yamlBlob_ = toRelativeTaggedYamlBlob([id: id_] + state_, java.nio.file.Paths.get(yamlFilename))

          [id_, yamlBlob_, yamlFilename]
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
    tuple val(id), val(yamlBlob), val(yamlFile)
  output:
    tuple val(id), path{[yamlFile]}
  script:
  """
  mkdir -p "\$(dirname '${yamlFile}')"
  echo "Storing state as yaml"
  echo '${yamlBlob}' > '${yamlFile}'
  """
}


// this assumes that the state contains no other values other than those specified in the config
def publishStatesByConfig(Map args) {
  def config = args.get("config")
  assert config != null : "publishStatesByConfig: config must be specified"

  def key_ = args.get("key", config.name)
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

          // the processed state is a list of [key, value, inputPath, outputFilename] tuples, where
          //   - key is a String
          //   - value is any object that can be serialized to a Yaml (so a String/Integer/Long/Double/Boolean, a List, a Map, or a Path)
          //   - (key, value) are the tuples that will be saved to the state.yaml file
          def processedState =
            config.allArguments
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
                  return [[key: plainName_, value: value]]
                }
                // if the orig state does not contain this filename,
                // it's an optional argument for which the user specified
                // that it should not be returned as a state
                if (!origState_.containsKey(plainName_)) {
                  return []
                }
                return [[key: plainName_, value: value]]
              }
              
          
          def updatedState_ = processedState.collectEntries{[it.key, it.value]}
          
          // convert state to yaml blob
          def yamlBlob_ = toTaggedYamlBlob([id: id_] + updatedState_)

          [id_, yamlBlob_, yamlFilename]
        }
        | publishStatesProc
    emit: input_ch
  }
  return publishStatesSimpleWf
}