/**
 * Split parameters for arguments that accept multiple values using their separator
 *
 * @param paramList A Map containing parameters to split.
 * @param config A Map of the Viash configuration. This Map can be generated from the config file
 *               using the readConfig() function.
 *
 * @return A Map of parameters where the parameter values have been split into a list using
 *         their seperator.
 */
Map<String, Object> _splitParams(Map<String, Object> parValues, Map config){
  def parsedParamValues = parValues.collectEntries { parName, parValue ->
    def parameterSettings = config.allArguments.find({it.plainName == parName})

    if (!parameterSettings) {
      // if argument is not found, do not alter 
      return [parName, parValue]
    }
    if (parameterSettings.multiple) { // Check if parameter can accept multiple values
      if (parValue instanceof Collection) {
          parValue = parValue.collect{it instanceof String ? it.split(parameterSettings.multiple_sep) : it }
      } else if (parValue instanceof String) {
          parValue = parValue.split(parameterSettings.multiple_sep)
      } else if (parValue == null) {
          parValue = []
      } else {
          parValue = [ parValue ]
      }
      parValue = parValue.flatten()
    }
    // For all parameters check if multiple values are only passed for
    // arguments that allow it. Quietly simplify lists of length 1.
    if (!parameterSettings.multiple && parValue instanceof Collection) {
      assert parValue.size() == 1 : 
      "Error: argument ${parName} has too many values.\n" +
      "  Expected amount: 1. Found: ${parValue.size()}"
      parValue = parValue[0]
    }
    [parName, parValue]
  }
  return parsedParamValues
}
