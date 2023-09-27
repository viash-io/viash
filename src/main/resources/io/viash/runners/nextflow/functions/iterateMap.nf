/**
  * Recursively apply a function over the leaves of an object.
  * @param obj The object to iterate over.
  * @param fun The function to apply to each value.
  * @return The object with the function applied to each value.
  */
def iterateMap(obj, fun) {
  if (obj instanceof List && obj !instanceof String) {
    return obj.collect{item ->
      iterateMap(item, fun)
    }
  } else if (obj instanceof Map) {
    return obj.collectEntries{key, item ->
      [key.toString(), iterateMap(item, fun)]
    }
  } else {
    return fun(obj)
  }
}
