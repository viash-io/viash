/**
 * Cast parameters to the correct type as defined in the Viash config
 *
 * @param parValues A Map of input arguments.
 *
 * @return The input arguments that have been cast to the type from the viash config.
 */
private Map<String, Object> _castParamTypes(Map<String, Object> parValues, Map config) {
  // Cast the input to the correct type according to viash config
  def castParValues = parValues.collectEntries({ parName, parValue ->
    paramSettings = config.functionality.allArguments.find({it.plainName == parName})
    // dont parse parameters like publish_dir ( in which case paramSettings = null)
    parType = paramSettings ? paramSettings.get("type", null) : null
    if (parValue !instanceof Collection) {
      parValue = [parValue]
    }
    if (parType == "file" && ((paramSettings.direction != null ? paramSettings.direction : "input") == "input")) {
      parValue = parValue.collect{ path ->
        if (path !instanceof String) {
          path
        } else {
          file(path)
        }
      }
    } else if (parType == "integer") {
      parValue = parValue.collect{it as Integer}
    } else if (parType == "double") {
      parValue = parValue.collect{it as Double}
    } else if (parType == "boolean" || 
                parType == "boolean_true" || 
                parType == "boolean_false") {
      parValue = parValue.collect{it as Boolean}
    }

    // simplify list to value if need be
    if (paramSettings && !paramSettings.multiple) {
      assert parValue.size() == 1 : 
        "Error: argument ${parName} has too many values.\n" +
        "  Expected amount: 1. Found: ${parValue.size()}"
      parValue = parValue[0]
    }
    [parName, parValue]
  })
  return castParValues
}

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
  def castParamValues = _castParamTypes(splitParamValues, config)

  // Check if any unexpected arguments were passed
  def knownParams = config.functionality.allArguments.collect({it.plainName}) + ["publishDir", "publish_dir"]
  castParamValues.each({parName, parValue ->
      assert parName in knownParams: "Unknown parameter. Parameter $parName should be in $knownParams"
  })
  return castParamValues
}
