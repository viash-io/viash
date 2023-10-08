def _checkArgumentType(String stage, Map par, Object value, String id, String key) {
  // expectedClass will only be != null if value is not of the expected type
  def expectedClass = null
  
  if (!par.required && value == null) {
    expectedClass = null
  } else if (par.multiple) {
    if (par.type == "file" && stage == "output" && par.direction == "input" && value instanceof String) {
      value = file(value, hidden: true)
    }
    if (value !instanceof List) {
      value = [value]
    }
    try {
      value = value.collect { listVal ->
        _checkArgumentType(stage, par + [multiple: false], listVal, id, key)
      }
    } catch (Exception e) {
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
      if (value instanceof String) {
        value = file(value, hidden: true)
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