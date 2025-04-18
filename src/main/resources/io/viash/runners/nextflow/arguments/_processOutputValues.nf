Map _checkValidOutputArgument(Map outputs, Map config, String id, String key) {
  if (!workflow.stubRun) {
    outputs = outputs.collectEntries { name, value ->
      def par = config.allArguments.find { it.plainName == name && it.direction == "output" }
      assert par != null : "Error in module '${key}' id '${id}': '${name}' is not a valid output argument"
      
      value = _checkArgumentType("output", par, value, "in module '$key' id '$id'")
      
      [ name, value ]
    }
  }
  return outputs
}

void _checkAllRequiredOuputsPresent(Map outputs, Map config, String id, String key) {
  if (!workflow.stubRun) {
    config.allArguments.each { arg ->
      if (arg.direction == "output" && arg.required) {
        assert outputs.containsKey(arg.plainName) && outputs.get(arg.plainName) != null : 
          "Error in module '${key}' id '${id}': required output argument '${arg.plainName}' is missing"
      }
    }
  }
}