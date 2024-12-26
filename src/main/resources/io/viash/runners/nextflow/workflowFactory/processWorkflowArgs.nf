def processWorkflowArgs(Map args, Map defaultWfArgs, Map meta) {
  // override defaults with args
  def workflowArgs = defaultWfArgs + args

  // check whether 'key' exists
  assert workflowArgs.containsKey("key") : "Error in module '${meta.config.name}': key is a required argument"

  // if 'key' is a closure, apply it to the original key
  if (workflowArgs["key"] instanceof Closure) {
    workflowArgs["key"] = workflowArgs["key"](meta.config.name)
  }
  def key = workflowArgs["key"]
  assert key instanceof CharSequence : "Expected process argument 'key' to be a String. Found: class ${key.getClass()}"
  assert key ==~ /^[a-zA-Z_]\w*$/ : "Error in module '$key': Expected process argument 'key' to consist of only letters, digits or underscores. Found: ${key}"

  // check for any unexpected keys
  def expectedKeys = ["key", "directives", "auto", "filter", "runIf", "fromState", "toState", "args", "debug"]
  def unexpectedKeys = workflowArgs.keySet() - expectedKeys
  assert unexpectedKeys.isEmpty() : "Error in module '$key': unexpected arguments to the '.run()' function: '${unexpectedKeys.join("', '")}'"

  // check whether directives exists and apply defaults
  assert workflowArgs.containsKey("directives") : "Error in module '$key': directives is a required argument"
  assert workflowArgs["directives"] instanceof Map : "Error in module '$key': Expected process argument 'directives' to be a Map. Found: class ${workflowArgs['directives'].getClass()}"
  workflowArgs["directives"] = processDirectives(defaultWfArgs.directives + workflowArgs["directives"])

  // check whether directives exists and apply defaults
  assert workflowArgs.containsKey("auto") : "Error in module '$key': auto is a required argument"
  assert workflowArgs["auto"] instanceof Map : "Error in module '$key': Expected process argument 'auto' to be a Map. Found: class ${workflowArgs['auto'].getClass()}"
  workflowArgs["auto"] = processAuto(defaultWfArgs.auto + workflowArgs["auto"])

  // auto define publish, if so desired
  if (workflowArgs.auto.publish == true && (workflowArgs.directives.publishDir != null ? workflowArgs.directives.publishDir : [:]).isEmpty()) {
    // can't assert at this level thanks to the no_publish profile
    // assert params.containsKey("publishDir") || params.containsKey("publish_dir") : 
    //   "Error in module '${workflowArgs['key']}': if auto.publish is true, params.publish_dir needs to be defined.\n" +
    //   "  Example: params.publish_dir = \"./output/\""
    def publishDir = getPublishDir()
    
    if (publishDir != null) {
      workflowArgs.directives.publishDir = [[ 
        path: publishDir, 
        saveAs: "{ it.startsWith('.') ? null : it }", // don't publish hidden files, by default
        mode: "copy"
      ]]
    }
  }

  // auto define transcript, if so desired
  if (workflowArgs.auto.transcript == true) {
    // can't assert at this level thanks to the no_publish profile
    // assert params.containsKey("transcriptsDir") || params.containsKey("transcripts_dir") || params.containsKey("publishDir") || params.containsKey("publish_dir") : 
    //   "Error in module '${workflowArgs['key']}': if auto.transcript is true, either params.transcripts_dir or params.publish_dir needs to be defined.\n" +
    //   "  Example: params.transcripts_dir = \"./transcripts/\""
    def transcriptsDir = 
      params.containsKey("transcripts_dir") ? params.transcripts_dir : 
      params.containsKey("transcriptsDir") ? params.transcriptsDir : 
      params.containsKey("publish_dir") ? params.publish_dir + "/_transcripts" :
      params.containsKey("publishDir") ? params.publishDir + "/_transcripts" : 
      null
    if (transcriptsDir != null) {
      def timestamp = nextflow.Nextflow.getSession().getWorkflowMetadata().start.format('yyyy-MM-dd_HH-mm-ss')
      def transcriptsPublishDir = [ 
        path: "$transcriptsDir/$timestamp/\${task.process.replaceAll(':', '-')}/\${id}/",
        saveAs: "{ it.startsWith('.') ? it.replaceAll('^.', '') : null }", 
        mode: "copy"
      ]
      def publishDirs = workflowArgs.directives.publishDir != null ? workflowArgs.directives.publishDir : null ? workflowArgs.directives.publishDir : []
      workflowArgs.directives.publishDir = publishDirs + transcriptsPublishDir
    }
  }

  // if this is a stubrun, remove certain directives?
  if (workflow.stubRun) {
    workflowArgs.directives.keySet().removeAll(["publishDir", "cpus", "memory", "label"])
  }

  for (nam in ["filter", "runIf"]) {
    if (workflowArgs.containsKey(nam) && workflowArgs[nam]) {
      assert workflowArgs[nam] instanceof Closure : "Error in module '$key': Expected process argument '$nam' to be null or a Closure. Found: class ${workflowArgs[nam].getClass()}"
    }
  }

  // check fromState
  workflowArgs["fromState"] = _processFromState(workflowArgs.get("fromState"), key, meta.config)

  // check toState
  workflowArgs["toState"] = _processToState(workflowArgs.get("toState"), key, meta.config)

  // return output
  return workflowArgs
}

def _processFromState(fromState, key_, config_) {
  assert fromState == null || fromState instanceof Closure || fromState instanceof Map || fromState instanceof List :
    "Error in module '$key_': Expected process argument 'fromState' to be null, a Closure, a Map, or a List. Found: class ${fromState.getClass()}"
  if (fromState == null) {
    return null
  }
  
  // if fromState is a List, convert to map
  if (fromState instanceof List) {
    // check whether fromstate is a list[string]
    assert fromState.every{it instanceof CharSequence} : "Error in module '$key_': fromState is a List, but not all elements are Strings"
    fromState = fromState.collectEntries{[it, it]}
  }

  // if fromState is a map, convert to closure
  if (fromState instanceof Map) {
    // check whether fromstate is a map[string, string]
    assert fromState.values().every{it instanceof CharSequence} : "Error in module '$key_': fromState is a Map, but not all values are Strings"
    assert fromState.keySet().every{it instanceof CharSequence} : "Error in module '$key_': fromState is a Map, but not all keys are Strings"
    def fromStateMap = fromState.clone()
    def requiredInputNames = meta.config.allArguments.findAll{it.required && it.direction == "Input"}.collect{it.plainName}
    // turn the map into a closure to be used later on
    fromState = { it ->
      def state = it[1]
      assert state instanceof Map : "Error in module '$key_': the state is not a Map"
      def data = fromStateMap.collectMany{newkey, origkey ->
        // check whether newkey corresponds to a required argument
        if (state.containsKey(origkey)) {
          [[newkey, state[origkey]]]
        } else if (!requiredInputNames.contains(origkey)) {
          []
        } else {
          throw new Exception("Error in module '$key_': fromState key '$origkey' not found in current state")
        }
      }.collectEntries()
      data
    }
  }
  
  return fromState
}

def _processToState(toState, key_, config_) {
  if (toState == null) {
    toState = { tup -> tup[1] }
  }

  // toState should be a closure, map[string, string], or list[string]
  assert toState instanceof Closure || toState instanceof Map || toState instanceof List :
    "Error in module '$key_': Expected process argument 'toState' to be a Closure, a Map, or a List. Found: class ${toState.getClass()}"

  // if toState is a List, convert to map
  if (toState instanceof List) {
    // check whether toState is a list[string]
    assert toState.every{it instanceof CharSequence} : "Error in module '$key_': toState is a List, but not all elements are Strings"
    toState = toState.collectEntries{[it, it]}
  }

  // if toState is a map, convert to closure
  if (toState instanceof Map) {
    // check whether toState is a map[string, string]
    assert toState.values().every{it instanceof CharSequence} : "Error in module '$key_': toState is a Map, but not all values are Strings"
    assert toState.keySet().every{it instanceof CharSequence} : "Error in module '$key_': toState is a Map, but not all keys are Strings"
    def toStateMap = toState.clone()
    def requiredOutputNames = config_.allArguments.findAll{it.required && it.direction == "Output"}.collect{it.plainName}
    // turn the map into a closure to be used later on
    toState = { it ->
      def output = it[1]
      def state = it[2]
      assert output instanceof Map : "Error in module '$key_': the output is not a Map"
      assert state instanceof Map : "Error in module '$key_': the state is not a Map"
      def extraEntries = toStateMap.collectMany{newkey, origkey ->
        // check whether newkey corresponds to a required argument
        if (output.containsKey(origkey)) {
          [[newkey, output[origkey]]]
        } else if (!requiredOutputNames.contains(origkey)) {
          []
        } else {
          throw new Exception("Error in module '$key_': toState key '$origkey' not found in current output")
        }
      }.collectEntries()
      state + extraEntries
    }
  }

  return toState
}
