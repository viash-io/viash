// This helper file will be deprecated soon
preprocessInputsDeprecationWarningPrinted = false

def preprocessInputsDeprecationWarning() {
  if (!preprocessInputsDeprecationWarningPrinted) {
    preprocessInputsDeprecationWarningPrinted = true
    System.err.println("Warning: preprocessInputs() is deprecated and will be removed in Viash 0.9.0.")
  }
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
  preprocessInputsDeprecationWarning()

  def config = args.config
  assert config instanceof Map : 
    "Error in preprocessInputs: config must be a map. " +
    "Expected class: Map. Found: config.getClass() is ${config.getClass()}"
  def key_ = args.key ?: config.name

  // Get different parameter types (used throughout this function)
  def defaultArgs = config.allArguments
    .findAll { it.containsKey("default") }
    .collectEntries { [ it.plainName, it.default ] }

  map { tup ->
    def id = tup[0]
    def data = tup[1]
    def passthrough = tup.drop(2)

    def new_data = (defaultArgs + data).collectEntries { name, value ->
      def par = config.allArguments.find { it.plainName == name && (it.direction == "input" || it.type == "file") }
      
      if (par != null) {
        value = _checkArgumentType("input", par, value, "in module '$key_' id '$id'")
      }

      [ name, value ]
    }

    [ id, new_data ] + passthrough
  }
}
