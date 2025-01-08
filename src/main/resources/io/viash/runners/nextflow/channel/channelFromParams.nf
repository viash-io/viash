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
  // todo: fetch key from run args
  def key_ = config.name
  
  /* parse regular parameters (not in param_list)  */
  /*************************************************/
  def globalParams = config.allArguments
    .findAll { params.containsKey(it.plainName) }
    .collectEntries { [ it.plainName, params[it.plainName] ] }
  def globalID = params.get("id", null)

  /* process params_list arguments */
  /*********************************/
  def paramList = params.containsKey("param_list") && params.param_list != null ?
    params.param_list : []
  // if (paramList instanceof String) {
  //   paramList = [paramList]
  // }
  // def paramSets = paramList.collectMany{ _parseParamList(it, config) }
  // TODO: be able to process param_list when it is a list of strings
  def paramSets = _parseParamList(paramList, config)
  if (paramSets.isEmpty()) {
    paramSets = [[null, [:]]]
  }

  /* combine arguments into channel */
  /**********************************/
  def processedParams = paramSets.indexed().collect{ index, tup ->
    // Process ID
    def id = tup[0] ?: globalID
  
    if (workflow.stubRun && !id) {
      // if stub run, explicitly add an id if missing
      id = "stub${index}"
    }
    assert id != null: "Each parameter set should have at least an 'id'"

    // Process params
    def parValues = globalParams + tup[1]
    // // Remove parameters which are null, if the default is also null
    // parValues = parValues.collectEntries{paramName, paramValue ->
    //   parameterSettings = config.functionality.allArguments.find({it.plainName == paramName})
    //   if ( paramValue != null || parameterSettings.get("default", null) != null ) {
    //     [paramName, paramValue]
    //   }
    // }
    parValues = parValues.collectEntries { name, value ->
      def par = config.allArguments.find { it.plainName == name && (it.direction == "input" || it.type == "file") }
      assert par != null : "Error in module '${key_}' id '${id}': '${name}' is not a valid input argument"

      if (par == null) {
        return [:]
      }
      value = _checkArgumentType("input", par, value, "in module '$key_' id '$id'")

      [ name, value ]
    }

    [id, parValues]
  }

  // Check if ids (first element of each list) is unique
  _checkUniqueIds(processedParams)
  return processedParams
}


private _channelFromParams(Map params, Map config) {
  def processedParams = _paramsToParamSets(params, config)
  return Channel.fromList(processedParams)
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
  log.warn "channelFromParams is deprecated and will be removed in Viash 0.10.0. ",
    "Nextflow workflows can now be build into standalone components where parsed parameters ",
    "are automatically provided to the input channel."
  return _channelFromParams(params, config) 
}
