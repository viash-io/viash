/**
 * Resolve the file paths in the parameters relative to given path
 *
 * @param paramList A Map containing parameters to process.
 *                  This function assumes that files are still of type String.
 * @param config A Map of the Viash configuration. This Map can be generated from the config file
 *               using the readConfig() function.
 * @param relativeTo path of a file to resolve the parameters values to.
 *
 * @return A map of parameters where the location of the input file parameters have been resolved
 *         resolved relatively to the provided path.
 */
private Map<String, Object> _resolvePathsRelativeTo(Map paramList, Map config, String relativeTo) {
  paramList.collectEntries { parName, parValue ->
    argSettings = config.functionality.allArguments.find{it.plainName == parName}
    if (argSettings && argSettings.type == "file" && argSettings.direction == "input") {
      if (parValue instanceof Collection) {
        parValue = parValue.collect({path -> 
          path !instanceof String ? path : file(_getChild(relativeTo, path))
        })
      } else {
        parValue = parValue !instanceof String ? path : file(_getChild(relativeTo, parValue))
      }
    }
    [parName, parValue]
  }
}

/**
 * Parse nextflow parameters based on settings defined in a viash config 
 * and return a nextflow channel.
 *
 * @param params Input parameters from nextflow.
 * @param config A Map of the Viash configuration. This Map can be generated from the config file
 *               using the readConfig() function.
 *
 * @return A list of parameter sets that were parsed from the 'param_list' argument value.
 */
private List<Tuple2<String, Map>> _parseParamListArguments(Map params, Map config){
  // first try to guess the format (if not set in params)
  def paramListFormat = _guessParamListFormat(params)

  // get the correct parser function for the detected params_list format
  def paramListParsers = [ 
    "csv": {[it, readCsv(it)]},
    "json": {[it, readJson(it)]},
    "yaml": {[it, readYaml(it)]},
    "yaml_blob": {[null, readYamlBlob(it)]},
    "asis": {[null, it]},
    "none": {[null, [[:]]]}
  ]
  assert paramListParsers.containsKey(paramListFormat):
    "Format of provided --param_list not recognised.\n" +
    "You can use '--param_list_format' to manually specify the format.\n" +
    "Found: '$paramListFormat'. Expected: one of 'csv', 'json', "+
    "'yaml', 'yaml_blob', 'asis' or 'none'"
  def paramListParser = paramListParsers.get(paramListFormat)

  // fetch multi param inputs
  def paramListOut = paramListParser(params.containsKey("param_list") ? params.param_list : "")
  // multiFile is null if the value passed to param_list was not a file (e.g a blob)
  // If the value was indeed a file, multiFile contains the location that file (used later).
  def paramListFile = paramListOut[0]
  def paramSets = paramListOut[1] // these are the actual parameters from reading the blob/file

  // data checks
  assert paramSets instanceof List: "--param_list should contain a list of maps"
  for (value in paramSets) {
    assert value instanceof Map: "--param_list should contain a list of maps"
  }

  // id is argument
  def idIsArgument = config.functionality.allArguments.find({it.plainName == "id"}) != null

  // Reformat from List<Map> to List<Tuple2<String, Map>> by adding the ID as first element of a Tuple2
  paramSets = paramSets.collect({ paramValues ->
    def paramId = paramValues.id
    if (!idIsArgument) {
      paramValues = paramValues.findAll{k, v -> k != "id"}
    }
    [paramId, paramValues]
  })

  // Split parameters with 'multiple: true'
  paramSets = paramSets.collect({ id, paramValues ->
    def splitParamValues = _splitParams(paramValues, config)
    [id, splitParamValues]
  })
  
  // The paths of input files inside a param_list file may have been specified relatively to the
  // location of the param_list file. These paths must be made absolute.
  if (paramListFile){
    paramSets = paramSets.collect({ id, paramValues ->
      def relativeParamValues = _resolvePathsRelativeTo(paramValues, config, paramListFile)
      [id, relativeParamValues]
    })
  }

  return paramSets
}

/**
 * Parse nextflow parameters based on settings defined in a viash config.
 * Return a list of parameter sets, each parameter set corresponding to 
 * an event in a nextflow channel. The output from this function can be used
 * with Channel.fromList to create a nextflow channel with Vdsl3 formatted 
 * events.
 *
 * This function performs:
 *   - A filtering of the params which can be found in the config file.
 *   - Process the params_list argument which allows a user to to initialise 
 *     a Vsdl3 channel with multiple parameter sets. Possible formats are 
 *     csv, json, yaml, or simply a yaml_blob. A csv should have column names 
 *     which correspond to the different arguments of this pipeline. A json or a yaml
 *     file should be a list of maps, each of which has keys corresponding to the
 *     arguments of the pipeline. A yaml blob can also be passed directly as a parameter.
 *     When passing a csv, json or yaml, relative path names are relativized to the
 *     location of the parameter file.
 *   - Combine the parameter sets into a vdsl3 Channel.
 *
 * @param params Input parameters. Can optionaly contain a 'param_list' key that
 *               provides a list of arguments that can be split up into multiple events
 *               in the output channel possible formats of param_lists are: a csv file, 
 *               json file, a yaml file or a yaml blob. Each parameters set (event) must
 *               have a unique ID.
 * @param config A Map of the Viash configuration. This Map can be generated from the config file
 *               using the readConfig() function.
 * 
 * @return A list of parameters with the first element of the event being
 *         the event ID and the second element containing a map of the parsed parameters.
 */
 
private List<Tuple2<String, Map<String, Object>>> _paramsToParamSets(Map params, Map config){
  /* parse regular parameters (not in param_list)  */
  /*************************************************/
  def globalParams = config.functionality.allArguments
    .findAll { params.containsKey(it.plainName) }
    .collectEntries { [ it.plainName, params[it.plainName] ] }
  def globalID = params.get("id", null)
  def globalParamsValues = applyConfigToOneParameterSet(globalParams, config)

  /* process params_list arguments */
  /*********************************/
  def paramSets = _parseParamListArguments(params, config)
  def parameterSetsWithConfigApplied = applyConfig(paramSets, config)

  /* combine arguments into channel */
  /**********************************/
  def processedParams = parameterSetsWithConfigApplied.indexed().collect{ index, paramSet ->
    def id = paramSet[0]
    def parValues = paramSet[1]
    id = [id, globalID].find({it != null}) // first non-null element
  
    if (workflow.stubRun) {
      // if stub run, explicitly add an id if missing
      id = id ? id : "stub" + index
    }
    assert id != null: "Each parameter set should have at least an 'id'"
    // Add regular parameters together with parameters passed with 'param_list'
    def combinedArgsValues = globalParamsValues + parValues

    // Remove parameters which are null, if the default is also null
    combinedArgsValues = combinedArgsValues.collectEntries{paramName, paramValue ->
      parameterSettings = config.functionality.allArguments.find({it.plainName == paramName})
      if ( paramValue != null || parameterSettings.get("default", null) != null ) {
        [paramName, paramValue]
      }
    }
    [id, combinedArgsValues]
  }

  // Check if ids (first element of each list) is unique
  _checkUniqueIds(processedParams)
  return processedParams
}

/**
 * Parse nextflow parameters based on settings defined in a viash config 
 * and return a nextflow channel.
 * 
 * @param params Input parameters. Can optionaly contain a 'param_list' key that
 *               provides a list of arguments that can be split up into multiple events
 *               in the output channel possible formats of param_lists are: a csv file, 
 *               json file, a yaml file or a yaml blob. Each parameters set (event) must
 *               have a unique ID.
 * @param config A Map of the Viash configuration. This Map can be generated from the config file
 *               using the readConfig() function.
 * 
 * @return A nextflow Channel with events. Events are formatted as a tuple that contains 
 *         first contains the ID of the event and as second element holds a parameter map.
 *       
 *
 */
def channelFromParams(Map params, Map config) {
  processedParams = _paramsToParamSets(params, config)
  return Channel.fromList(processedParams)
}
