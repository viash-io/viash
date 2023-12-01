/**
  * Figure out the param list format based on the file extension
  *
  * @param param_list A String containing the path to the parameter list file.
  *
  * @return A String containing the format of the parameter list file.
  */
def _paramListGuessFormat(param_list) {
  if (param_list !instanceof String) {
    "asis"
  } else if (param_list.endsWith(".csv")) {
    "csv"
  } else if (param_list.endsWith(".json") || param_list.endsWith(".jsn")) {
    "json"
  } else if (param_list.endsWith(".yaml") || param_list.endsWith(".yml")) {
    "yaml"
  } else {
    "yaml_blob"
  }
}


/**
  * Read the param list
  * 
  * @param param_list One of the following:
  *   - A String containing the path to the parameter list file (csv, json or yaml),
  *   - A yaml blob of a list of maps (yaml_blob),
  *   - Or a groovy list of maps (asis).
  * @param config A Map of the Viash configuration.
  * 
  * @return A List of Maps containing the parameters.
  */
def _parseParamList(param_list, Map config) {
  // first determine format by extension
  def paramListFormat = _paramListGuessFormat(param_list)

  def paramListPath = (paramListFormat != "asis" && paramListFormat != "yaml_blob") ?
    file(param_list, hidden: true) :
    null

  // get the correct parser function for the detected params_list format
  def paramSets = []
  if (paramListFormat == "asis") {
    paramSets = param_list
  } else if (paramListFormat == "yaml_blob") {
    paramSets = readYamlBlob(param_list)
  } else if (paramListFormat == "yaml") {
    paramSets = readYaml(paramListPath)
  } else if (paramListFormat == "json") {
    paramSets = readJson(paramListPath)
  } else if (paramListFormat == "csv") {
    paramSets = readCsv(paramListPath)
  } else {
    error "Format of provided --param_list not recognised.\n" +
    "Found: '$paramListFormat'.\n" +
    "Expected: a csv file, a json file, a yaml file,\n" +
    "a yaml blob or a groovy list of maps."
  }

  // data checks
  assert paramSets instanceof List: "--param_list should contain a list of maps"
  for (value in paramSets) {
    assert value instanceof Map: "--param_list should contain a list of maps"
  }

  // id is argument
  def idIsArgument = config.functionality.allArguments.any{it.plainName == "id"}

  // Reformat from List<Map> to List<Tuple2<String, Map>> by adding the ID as first element of a Tuple2
  paramSets = paramSets.collect({ data ->
    def id = data.id
    if (!idIsArgument) {
      data = data.findAll{k, v -> k != "id"}
    }
    [id, data]
  })

  // Split parameters with 'multiple: true'
  paramSets = paramSets.collect({ id, data ->
    data = _splitParams(data, config)
    [id, data]
  })
  
  // The paths of input files inside a param_list file may have been specified relatively to the
  // location of the param_list file. These paths must be made absolute.
  if (paramListPath) {
    paramSets = paramSets.collect({ id, data ->
      def new_data = data.collectEntries{ parName, parValue ->
        def par = config.functionality.allArguments.find{it.plainName == parName}
        if (par && par.type == "file" && par.direction == "input") {
          if (parValue instanceof Collection) {
            parValue = parValue.collectMany{path -> 
              def x = _resolveSiblingIfNotAbsolute(path, paramListPath)
              x instanceof Collection ? x : [x]
            }
          } else {
            parValue = _resolveSiblingIfNotAbsolute(parValue, paramListPath) 
          }
        }
        [parName, parValue]
      }
      [id, new_data]
    })
  }

  return paramSets
}
