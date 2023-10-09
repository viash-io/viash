/**
 * Apply the argument settings specified in a Viash config to a single parameter set.
 *    - Split the parameter values according to their seperator if 
 *       the parameter accepts multiple values
 *    - Cast the parameters to their corect types.
 *    - Assertions:
 *        ~ Check if any unknown parameters are found
 * 
 * @param paramValues A Map of parameter to be processed. All parameters must 
 *                    also be specified in the Viash config.
 * @param config: A Map of the Viash configuration. This Map can be generated from 
 *                the config file using the readConfig() function.
 * @return The input parameters that have been processed.
 */
Map<String, Object> applyConfigToOneParameterSet(Map<String, Object> paramValues, Map config){
  def splitParamValues = _splitParams(paramValues, config)
  def castParamValues = paramValues.collectEntries({ parName, parValue ->
    def paramSettings = config.functionality.allArguments.find({it.plainName == parName})
    // dont parse parameters like publish_dir ( in which case paramSettings = null)

    if (paramSettings) {
      // check if the parameter is a list
      def tupleId = null
      def componentKey = null
      parValue = _checkArgumentType("input", paramSettings, parValue, tupleId, componentKey)
    }

    [parName, parValue]
  })

  // Check if any unexpected arguments were passed
  def knownParams = config.functionality.allArguments.collect({it.plainName}) + ["publishDir", "publish_dir"]
  castParamValues.each({parName, parValue ->
      assert parName in knownParams: "Unknown parameter. Parameter $parName should be in $knownParams"
  })
  return castParamValues
}
