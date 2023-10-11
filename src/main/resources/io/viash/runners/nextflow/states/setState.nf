def setState(fun) {
  assert fun instanceof Closure || fun instanceof Map || fun instanceof List :
    "Error in setState: Expected process argument to be a Closure, a Map, or a List. Found: class ${fun.getClass()}"

  // if fun is a List, convert to map
  if (fun instanceof List) {
    // check whether fun is a list[string]
    assert fun.every{it instanceof CharSequence} : "Error in setState: argument is a List, but not all elements are Strings"
    fun = fun.collectEntries{[it, it]}
  }

  // if fun is a map, convert to closure
  if (fun instanceof Map) {
    // check whether fun is a map[string, string]
    assert fun.values().every{it instanceof CharSequence} : "Error in setState: argument is a Map, but not all values are Strings"
    assert fun.keySet().every{it instanceof CharSequence} : "Error in setState: argument is a Map, but not all keys are Strings"
    def funMap = fun.clone()
    // turn the map into a closure to be used later on
    fun = { id_, state_ ->
      assert state_ instanceof Map : "Error in setState: the state is not a Map"
      funMap.collectMany{newkey, origkey ->
        if (state_.containsKey(origkey)) {
          [[newkey, state_[origkey]]]
        } else {
          []
        }
      }.collectEntries()
    }
  }

  map { tup ->
    def id = tup[0]
    def state = tup[1]
    def unfilteredState = fun(id, state)
    def newState = unfilteredState.findAll{key, val -> val != null}
    [id, newState] + tup.drop(2)
  }
}
