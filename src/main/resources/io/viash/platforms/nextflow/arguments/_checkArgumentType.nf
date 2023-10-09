class UnexpectedArgumentTypeException extends Exception {
  String key
  String id
  String stage
  String plainName
  String expectedClass
  String foundClass

  UnexpectedArgumentTypeException(String key, String id, String stage, String plainName, String expectedClass, String foundClass) {
    super("Error${key ? " module '$key'" : ""}${id ? " id '$id'" : ""}:${stage ? " $stage" : "" } argument '${plainName}' has the wrong type. " +
      "Expected type: ${expectedClass}. Found type: ${foundClass}")
    this.key = key
    this.id = id
    this.stage = stage
    this.plainName = plainName
    this.expectedClass = expectedClass
    this.foundClass = foundClass
  }
}

/**
  * Checks if the given value is of the expected type. If not, an exception is thrown.
  *
  * @param stage The stage of the argument (input or output)
  * @param par The parameter definition
  * @param value The value to check
  * @param id The id of tuple where the data comes from. Can be null if unknown.
  * @param key The key of the module
  * @return The value, if it is of the expected type
  * @throws UnexpectedArgumentTypeException If the value is not of the expected type
*/
def _checkArgumentType(String stage, Map par, Object value, String id, String key) {
  // expectedClass will only be != null if value is not of the expected type
  def expectedClass = null
  def foundClass = null
  
  if (!par.required && value == null) {
    expectedClass = null
  } else if (par.multiple) {
    if (par.type == "file" && par.direction == "input" && value instanceof String) {
      value = file(value, hidden: true)
    }
    if (value !instanceof Collection) {
      value = [value]
    }
    try {
      value = value.collect { listVal ->
        _checkArgumentType(stage, par + [multiple: false], listVal, id, key)
      }
    } catch (UnexpectedArgumentTypeException e) {
      expectedClass = "List[${e.expectedClass}]"
      foundClass = "List[${e.foundClass}]"
    }
  } else if (par.type == "string") {
    if (value instanceof GString) {
      value = value.toString()
    }
    expectedClass = value instanceof String ? null : "String"
  } else if (par.type == "integer") {
    // TODO: allow this?
    if (value instanceof String && value.isInteger()) {
      value = value.toInteger()
    }
    if (value instanceof java.math.BigInteger) {
      value = value.intValue()
    }
    expectedClass = value instanceof Integer ? null : "Integer"
  } else if (par.type == "long") {
    if (value instanceof String && value.isLong()) {
      value = value.toLong()
    }
    if (value instanceof Integer) {
      value = value.toLong()
    }
    expectedClass = value instanceof Long ? null : "Long"
  } else if (par.type == "double") {
    if (value instanceof String && value.isDouble()) {
      value = value.toDouble()
    }
    if (value instanceof java.math.BigDecimal) {
      value = value.doubleValue()
    }
    if (value instanceof Float || value instanceof Integer || value instanceof Long) {
      value = value.toDouble()
    }
    expectedClass = value instanceof Double ? null : "Double"
  } else if (par.type == "boolean" | par.type == "boolean_true" | par.type == "boolean_false") {
    if (value instanceof String) {
      value = value.toLowerCase()
      if (value == "true") {
        value = true
      } else if (value == "false") {
        value = false
      }
    }
    expectedClass = value instanceof Boolean ? null : "Boolean"
  } else if (par.type == "file") {
    if (stage == "output" || par.direction == "input") {
      if (value instanceof String) {
        value = file(value, hidden: true)
      }
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
    if (foundClass == null) {
      foundClass = value.getClass().getName()
    }
    throw new UnexpectedArgumentTypeException(key, id, stage, par.plainName, expectedClass, foundClass)
  }
  
  return value
}