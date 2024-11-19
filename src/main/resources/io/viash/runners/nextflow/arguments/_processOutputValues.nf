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
