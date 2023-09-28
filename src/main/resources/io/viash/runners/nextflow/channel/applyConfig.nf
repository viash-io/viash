
/**
 * Apply the argument settings specified in a Viash config to a list of parameter sets.
 *    - Split the parameter values according to their seperator if 
 *       the parameter accepts multiple values
 *    - Cast the parameters to their corect types.
 *    - Assertions:
 *        ~ Check if any unknown parameters are found
 *        ~ Check if the ID of the parameter set is unique across all sets.
 * 
 * @return The input parameters that have been processed.
 */

List<Tuple> applyConfig(List<Tuple> parameterSets, Map config){
  def processedparameterSets = parameterSets.collect({ parameterSet ->
    def id = parameterSet[0]
    def paramValues = parameterSet[1]
    def passthrough = parameterSet.drop(2)
    def processedSet = applyConfigToOneParameterSet(paramValues, config)
    [id, processedSet] + passthrough
  })

  _checkUniqueIds(processedparameterSets)
  return processedparameterSets
}
