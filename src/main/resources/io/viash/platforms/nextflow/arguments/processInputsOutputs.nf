def typeCheck(String stage, Map par, Object value, String id, String key) {
  // expectedClass will only be != null if value is not of the expected type
  def expectedClass = null
  
  if (!par.required && value == null) {
    expectedClass = null
  } else if (par.multiple) {
    if (value instanceof List) {
      try {
        value = value.collect { listVal ->
          typeCheck(stage, par + [multiple: false], listVal, id, key)
        }
      } catch (Exception e) {
        expectedClass = "List[${par.type}]"
      }
    } else {
      expectedClass = "List[${par.type}]"
    }
  } else if (par.type == "string") {
    if (value instanceof GString) {
      value = value.toString()
    }
    expectedClass = value instanceof String ? null : "String"
  } else if (par.type == "integer") {
    expectedClass = value instanceof Integer ? null : "Integer"
  } else if (par.type == "long") {
    if (value instanceof Integer) {
      value = value.toLong()
    }
    expectedClass = value instanceof Long ? null : "Long"
  } else if (par.type == "double") {
    if (value instanceof java.math.BigDecimal) {
      value = value.doubleValue()
    }
    if (value instanceof Float || value instanceof Integer || value instanceof Long) {
      value = value.toDouble()
    }
    expectedClass = value instanceof Double ? null : "Double"
  } else if (par.type == "boolean" | par.type == "boolean_true" | par.type == "boolean_false") {
    expectedClass = value instanceof Boolean ? null : "Boolean"
  } else if (par.type == "file") {
    if (stage == "output" || par.direction == "input") {
      if (value instanceof File) {
        value = value.toPath()
      }
      expectedClass = value instanceof Path ? null : "Path"
    } else { // stage == "input" && par.direction == "output"
      if (value instanceof GString) {
        value = value.toString()
      }
      expectedClass = value instanceof String ? null : "String"
    }
  } else {
    expectedClass = par.type
  }

  if (expectedClass != null) {
    error "Error in module '${key}' id '${id}': ${stage} argument '${par.plainName}' has the wrong type. " +
      "Expected type: ${expectedClass}. Found type: ${value.getClass()}"
  }
  
  return value
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

      value = typeCheck("input", par, value, id, key)

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
      
      value = typeCheck("output", par, value, id, key)
      
      [ name, value ]
    }
  }
  return outputs
}
