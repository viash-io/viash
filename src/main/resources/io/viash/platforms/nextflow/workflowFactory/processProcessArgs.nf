// depends on: thisConfig, thisDefaultProcessArgs
def processProcessArgs(Map args) {
  // override defaults with args
  def processArgs = thisDefaultProcessArgs + args

  // check whether 'key' exists
  assert processArgs.containsKey("key") : "Error in module '${thisConfig.functionality.name}': key is a required argument"

  // if 'key' is a closure, apply it to the original key
  if (processArgs["key"] instanceof Closure) {
    processArgs["key"] = processArgs["key"](thisConfig.functionality.name)
  }
  def key = processArgs["key"]
  assert key instanceof CharSequence : "Expected process argument 'key' to be a String. Found: class ${key.getClass()}"
  assert key ==~ /^[a-zA-Z_]\w*$/ : "Error in module '$key': Expected process argument 'key' to consist of only letters, digits or underscores. Found: ${key}"

  // check for any unexpected keys
  def expectedKeys = ["key", "directives", "auto", "map", "mapId", "mapData", "mapPassthrough", "filter", "fromState", "toState", "args", "renameKeys", "debug"]
  def unexpectedKeys = processArgs.keySet() - expectedKeys
  assert unexpectedKeys.isEmpty() : "Error in module '$key': unexpected arguments to the '.run()' function: '${unexpectedKeys.join("', '")}'"

  // check whether directives exists and apply defaults
  assert processArgs.containsKey("directives") : "Error in module '$key': directives is a required argument"
  assert processArgs["directives"] instanceof Map : "Error in module '$key': Expected process argument 'directives' to be a Map. Found: class ${processArgs['directives'].getClass()}"
  processArgs["directives"] = processDirectives(thisDefaultProcessArgs.directives + processArgs["directives"])

  // check whether directives exists and apply defaults
  assert processArgs.containsKey("auto") : "Error in module '$key': auto is a required argument"
  assert processArgs["auto"] instanceof Map : "Error in module '$key': Expected process argument 'auto' to be a Map. Found: class ${processArgs['auto'].getClass()}"
  processArgs["auto"] = processAuto(thisDefaultProcessArgs.auto + processArgs["auto"])

  // auto define publish, if so desired
  if (processArgs.auto.publish == true && (processArgs.directives.publishDir != null ? processArgs.directives.publishDir : [:]).isEmpty()) {
    // can't assert at this level thanks to the no_publish profile
    // assert params.containsKey("publishDir") || params.containsKey("publish_dir") : 
    //   "Error in module '${processArgs['key']}': if auto.publish is true, params.publish_dir needs to be defined.\n" +
    //   "  Example: params.publish_dir = \"./output/\""
    def publishDir = getPublishDir()
    
    if (publishDir != null) {
      processArgs.directives.publishDir = [[ 
        path: publishDir, 
        saveAs: "{ it.startsWith('.') ? null : it }", // don't publish hidden files, by default
        mode: "copy"
      ]]
    }
  }

  // auto define transcript, if so desired
  if (processArgs.auto.transcript == true) {
    // can't assert at this level thanks to the no_publish profile
    // assert params.containsKey("transcriptsDir") || params.containsKey("transcripts_dir") || params.containsKey("publishDir") || params.containsKey("publish_dir") : 
    //   "Error in module '${processArgs['key']}': if auto.transcript is true, either params.transcripts_dir or params.publish_dir needs to be defined.\n" +
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
      def publishDirs = processArgs.directives.publishDir != null ? processArgs.directives.publishDir : null ? processArgs.directives.publishDir : []
      processArgs.directives.publishDir = publishDirs + transcriptsPublishDir
    }
  }

  // if this is a stubrun, remove certain directives?
  if (workflow.stubRun) {
    processArgs.directives.keySet().removeAll(["publishDir", "cpus", "memory", "label"])
  }

  for (nam in ["map", "mapId", "mapData", "mapPassthrough", "filter"]) {
    if (processArgs.containsKey(nam) && processArgs[nam]) {
      assert processArgs[nam] instanceof Closure : "Error in module '$key': Expected process argument '$nam' to be null or a Closure. Found: class ${processArgs[nam].getClass()}"
    }
  }

  // check fromState
  assert processArgs.containsKey("fromState") : "Error in module '$key': fromState is a required argument"
  def fromState = processArgs["fromState"]
  assert fromState == null || fromState instanceof Closure || fromState instanceof Map || fromState instanceof List :
    "Error in module '$key': Expected process argument 'fromState' to be null, a Closure, a Map, or a List. Found: class ${fromState.getClass()}"
  if (fromState) {
    // if fromState is a List, convert to map
    if (fromState instanceof List) {
      // check whether fromstate is a list[string]
      assert fromState.every{it instanceof CharSequence} : "Error in module '$key': fromState is a List, but not all elements are Strings"
      fromState = fromState.collectEntries{[it, it]}
    }

    // if fromState is a map, convert to closure
    if (fromState instanceof Map) {
      // check whether fromstate is a map[string, string]
      assert fromState.values().every{it instanceof CharSequence} : "Error in module '$key': fromState is a Map, but not all values are Strings"
      assert fromState.keySet().every{it instanceof CharSequence} : "Error in module '$key': fromState is a Map, but not all keys are Strings"
      def fromStateMap = fromState.clone()
      def requiredInputNames = thisConfig.functionality.allArguments.findAll{it.required && it.direction == "Input"}.collect{it.plainName}
      // turn the map into a closure to be used later on
      fromState = { it ->
        def state = it[1]
        assert state instanceof Map : "Error in module '$key': the state is not a Map"
        def data = fromStateMap.collectMany{newkey, origkey ->
          // check whether newkey corresponds to a required argument
          if (state.containsKey(origkey)) {
            [[newkey, state[origkey]]]
          } else if (!requiredInputNames.contains(origkey)) {
            []
          } else {
            throw new Exception("Error in module '$key': fromState key '$origkey' not found in current state")
          }
        }.collectEntries()
        data
      }
    }

    processArgs["fromState"] = fromState
  }

  // check toState
  def toState = processArgs["toState"]

  if (toState == null) {
    toState = { tup -> tup[1] }
  }

  // toState should be a closure, map[string, string], or list[string]
  assert toState instanceof Closure || toState instanceof Map || toState instanceof List :
    "Error in module '$key': Expected process argument 'toState' to be a Closure, a Map, or a List. Found: class ${toState.getClass()}"

  // if toState is a List, convert to map
  if (toState instanceof List) {
    // check whether toState is a list[string]
    assert toState.every{it instanceof CharSequence} : "Error in module '$key': toState is a List, but not all elements are Strings"
    toState = toState.collectEntries{[it, it]}
  }

  // if toState is a map, convert to closure
  if (toState instanceof Map) {
    // check whether toState is a map[string, string]
    assert toState.values().every{it instanceof CharSequence} : "Error in module '$key': toState is a Map, but not all values are Strings"
    assert toState.keySet().every{it instanceof CharSequence} : "Error in module '$key': toState is a Map, but not all keys are Strings"
    def toStateMap = toState.clone()
    def requiredOutputNames = thisConfig.functionality.allArguments.findAll{it.required && it.direction == "Output"}.collect{it.plainName}
    // turn the map into a closure to be used later on
    toState = { it ->
      def output = it[1]
      def state = it[2]
      assert output instanceof Map : "Error in module '$key': the output is not a Map"
      assert state instanceof Map : "Error in module '$key': the state is not a Map"
      def extraEntries = toStateMap.collectMany{newkey, origkey ->
        // check whether newkey corresponds to a required argument
        if (output.containsKey(origkey)) {
          [[newkey, output[origkey]]]
        } else if (!requiredOutputNames.contains(origkey)) {
          []
        } else {
          throw new Exception("Error in module '$key': toState key '$origkey' not found in current output")
        }
      }.collectEntries()
      state + extraEntries
    }
  }

  processArgs["toState"] = toState

  // return output
  return processArgs
}
