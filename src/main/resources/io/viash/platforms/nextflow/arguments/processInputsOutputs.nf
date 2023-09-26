boolean typeCheck(String stage, Map par, Object x) {
  if (!par.required && x == null) {
    return true
  } else if (par.multiple) {
    x instanceof List && x.every { typeCheck(stage, par + [multiple: false], it) }
  } else if (par.type == "string") {
    x instanceof CharSequence
  } else if (par.type == "integer") {
    x instanceof Integer
  } else if (par.type == "long") {
    x instanceof Integer || x instanceof Long
  } else if (par.type == "boolean") {
    x instanceof Boolean
  } else if (par.type == "file") {
    if (stage == "output") {
      x instanceof File || x instanceof Path
    } else if (par.direction == "input") {
      x instanceof File || x instanceof Path
    } else if (par.direction == "output") {
      x instanceof String
    }
  }
}

Map processInputs(Map inputs, Map config, String id, String key) {
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
      
      // if value is a gstring, turn it into a regular string
      if (value instanceof GString) {
        value = value.toString()
      }

      assert typeCheck("input", par, value) : 
        "Error in module '${key}' id '${id}': input argument '${name}' has the wrong type. " +
        "Expected type: ${par.multiple ? "List[${par.type}]" : par.type}. Found type: ${value.getClass()}"

      [ name, value ]
    }
  }
  return inputs
}

Map processOutputs(Map outputs, Map config, String id, String key) {
  if (!workflow.stubRun) {
    config.functionality.allArguments.each { arg ->
      if (arg.direction == "output" && arg.required) {
        assert outputs.containsKey(arg.plainName) && outputs.get(arg.plainName) != null : 
          "Error in module '${key}' id '${id}': required output argument '${arg.plainName}' is missing"
      }
    }

    outputs = outputs.collectEntries { name, value ->
      def par = config.functionality.allArguments.find { it.plainName == name && it.direction == "output" }
      assert par != null : "Error in module '${key}' id '${id}': '${name}' is not a valid output argument"
      
      // if value is a gstring, turn it into a regular string
      if (value instanceof GString) {
        value = value.toString()
      }

      assert typeCheck("output", par, value) : 
        "Error in module '${key}' id '${id}': output argument '${name}' has the wrong type. " +
        "Expected type: ${par.multiple ? "List[${par.type}]" : par.type}. Found type: ${value.getClass()}"
      
      [ name, value ]
    }
  }
  return outputs
}
