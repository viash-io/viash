
/**
 * Process a list of Vdsl3 formatted parameters and apply a Viash config to them:
 *    - Gather default parameters from the Viash config and make 
 *      sure that they are correctly formatted (see applyConfig method).
 *    - Format the input parameters (also using the applyConfig method).
 *    - Apply the default parameter to the input parameters.
 *    - Do some assertions:
 *        ~ Check if the event IDs in the channel are unique.
 *  
 * @param params A list of parameter sets as Tuples. The first element of the tuples
 *                must be a unique id of the parameter set, and the second element 
 *                must contain the parameters themselves. Optional extra elements 
 *                of the tuples will be passed to the output as is.
 * @param config A Map of the Viash configuration. This Map can be generated from 
 *                the config file using the readConfig() function.           
 *
 * @return A list of processed parameters sets as tuples.
 */

private List<Tuple> _preprocessInputsList(List<Tuple> params, Map config) {
  // Get different parameter types (used throughout this function)
  def defaultArgs = config.functionality.allArguments
    .findAll { it.containsKey("default") }
    .collectEntries { [ it.plainName, it.default ] }

  // Apply config to default parameters
  def parsedDefaultValues = applyConfigToOneParameterSet(defaultArgs, config)

  // Apply config to input parameters
  def parsedInputParamSets = applyConfig(params, config)

  // Merge two parameter sets together
  def parsedArgs = parsedInputParamSets.collect({ parsedInputParamSet ->
    def id = parsedInputParamSet[0]
    def parValues = parsedInputParamSet[1]
    def passthrough = parsedInputParamSet.drop(2)
    def parValuesWithDefault = parsedDefaultValues + parValues
    [id, parValuesWithDefault] + passthrough
  })
  _checkUniqueIds(parsedArgs)

  return parsedArgs
}

/**
 * Generate a nextflow Workflow that allows processing a channel of 
 * Vdsl3 formatted events and apply a Viash config to them:
 *    - Gather default parameters from the Viash config and make 
 *      sure that they are correctly formatted (see applyConfig method).
 *    - Format the input parameters (also using the applyConfig method).
 *    - Apply the default parameter to the input parameters.
 *    - Do some assertions:
 *        ~ Check if the event IDs in the channel are unique.
 * 
 * The events in the channel are formatted as tuples, with the 
 * first element of the tuples being a unique id of the parameter set, 
 * and the second element containg the the parameters themselves.
 * Optional extra elements of the tuples will be passed to the output as is.
 *
 * @param args A map that must contain a 'config' key that points
 *              to a parsed config (see readConfig()). Optionally, a
 *              'key' key can be provided which can be used to create a unique
 *              name for the workflow process.
 *
 * @return A workflow that allows processing a channel of Vdsl3 formatted events
 * and apply a Viash config to them.
 */
def preprocessInputs(Map args) {
  wfKey = args.key != null ? args.key : "preprocessInputs"
  config = args.config
  workflow preprocessInputsInstance {
    take: 
    input_ch

    main:
    assert config instanceof Map : 
      "Error in preprocessInputs: config must be a map. " +
      "Expected class: Map. Found: config.getClass() is ${config.getClass()}"

    output_ch = input_ch
      | toSortedList
      | map { paramList -> _preprocessInputsList(paramList, config) }
      | flatMap
    emit:
    output_ch
  }

  return preprocessInputsInstance.cloneWithName(wfKey)
}
