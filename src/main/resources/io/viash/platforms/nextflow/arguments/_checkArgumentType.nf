class UnexpectedArgumentTypeException extends Exception {
  String errorIdentifier
  String stage
  String plainName
  String expectedClass
  String foundClass
  
  // ${key ? " in module '$key'" : ""}${id ? " id '$id'" : ""}
  UnexpectedArgumentTypeException(String errorIdentifier, String stage, String plainName, String expectedClass, String foundClass) {
    super("Error${errorIdentifier ? " $errorIdentifier" : ""}:${stage ? " $stage" : "" } argument '${plainName}' has the wrong type. " +
      "Expected type: ${expectedClass}. Found type: ${foundClass}")
    this.errorIdentifier = errorIdentifier
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
  * @param errorIdentifier The identifier to use in the error message
  * @return The value, if it is of the expected type
  * @throws UnexpectedArgumentTypeException If the value is not of the expected type
*/
def _checkArgumentType(String stage, Map par, Object value, String errorIdentifier) {
  // expectedClass will only be != null if value is not of the expected type
  def expectedClass = null
  def foundClass = null
  
  // todo: split if need be
  
  if (!par.required && value == null) {
    expectedClass = null
  } else if (par.multiple) {
    if (value !instanceof Collection) {
      value = [value]
    }
    
    // split strings
    value = value.collectMany{ val ->
      if (val instanceof String) {
        // collect() to ensure that the result is a List and not simply an array
        val.split(par.multiple_sep).collect()
      } else {
        [val]
      }
    }

    // process globs
    if (par.type == "file" && par.direction == "input") {
      value = value.collect{ it instanceof String ? file(it, hidden: true) : it }.flatten()
    }

    // check types of elements in list
    try {
      value = value.collect { listVal ->
        _checkArgumentType(stage, par + [multiple: false], listVal, errorIdentifier)
      }
    } catch (UnexpectedArgumentTypeException e) {
      expectedClass = "List[${e.expectedClass}]"
      foundClass = "List[${e.foundClass}]"
    }
  } else if (par.type == "string") {
    // cast to string if need be. only cast if the value is a GString
    if (value instanceof GString) {
      value = value as String
    }
    expectedClass = value instanceof String ? null : "String"
  } else if (par.type == "integer") {
    // cast to integer if need be
    if (value !instanceof Integer) {
      try {
        value = value as Integer
      } catch (NumberFormatException e) {
        expectedClass = "Integer"
      }
    }
  } else if (par.type == "long") {
    // cast to long if need be
    if (value !instanceof Long) {
      try {
        value = value as Long
      } catch (NumberFormatException e) {
        expectedClass = "Long"
      }
    }
  } else if (par.type == "double") {
    // cast to double if need be
    if (value !instanceof Double) {
      try {
        value = value as Double
      } catch (NumberFormatException e) {
        expectedClass = "Double"
      }
    }
  } else if (par.type == "float") {
    // cast to float if need be
    if (value !instanceof Float) {
      try {
        value = value as Float
      } catch (NumberFormatException e) {
        expectedClass = "Float"
      }
    }
  } else if (par.type == "boolean" | par.type == "boolean_true" | par.type == "boolean_false") {
    // cast to boolean if need be
    if (value !instanceof Boolean) {
      try {
        value = value as Boolean
      } catch (Exception e) {
        expectedClass = "Boolean"
      }
    }
  } else if (par.type == "file" && (par.direction == "input" || stage == "output")) {
    // cast to path if need be
    if (value instanceof String) {
      value = file(value, hidden: true)
    }
    if (value instanceof File) {
      value = value.toPath()
    }
    expectedClass = value instanceof Path ? null : "Path"
  } else if (par.type == "file" && stage == "input" && par.direction == "output") {
    // cast to string if need be
    if (value !instanceof String) {
      try {
        value = value as String
      } catch (Exception e) {
        expectedClass = "String"
      }
    }
  } else {
    // didn't find a match for par.type
    expectedClass = par.type
  }

  if (expectedClass != null) {
    if (foundClass == null) {
      foundClass = value.getClass().getName()
    }
    throw new UnexpectedArgumentTypeException(errorIdentifier, stage, par.plainName, expectedClass, foundClass)
  }
  
  return value
}