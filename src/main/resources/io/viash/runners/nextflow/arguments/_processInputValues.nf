Map _processInputValues(Map inputs, Map config, String id, String key) {
  if (!workflow.stubRun) {
    config.functionality.allArguments.each { arg ->
      if (arg.required) {
        assert inputs.containsKey(arg.plainName) && inputs.get(arg.plainName) != null : 
          "Error in module '${key}' id '${id}': required input argument '${arg.plainName}' is missing"
      }
    }

    inputs = inputs.collectEntries { name, value ->
      def par = config.functionality.allArguments.find { it.plainName == name && (it.direction == "input" || it.type == "file") }
      assert par != null : "Error in module '${key}' id '${id}': '${name}' is not a valid input argument"

      value = _checkArgumentType("input", par, value, "in module '$key' id '$id'")

      [ name, value ]
    }
  }
  return inputs
}
