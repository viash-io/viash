def _debug(processArgs, debugKey) {
  if (processArgs.debug) {
    view { "process '${processArgs.key}' $debugKey tuple: $it"  }
  } else {
    map { it }
  }
}

// depends on: thisConfig, innerWorkflowFactory
def workflowFactory(Map args) {
  def processArgs = processProcessArgs(args)
  def key = processArgs["key"]
  def meta = nextflow.script.ScriptMeta.current()

  def workflowKey = key
  
  workflow workflowInstance {
    take:
    input_

    main:

    mid1_ = input_
      | _debug(processArgs, "input")
      | map { tuple ->
        tuple = tuple.clone()
        
        if (processArgs.map) {
          tuple = processArgs.map(tuple)
        }
        if (processArgs.mapId) {
          tuple[0] = processArgs.mapId(tuple[0])
        }
        if (processArgs.mapData) {
          tuple[1] = processArgs.mapData(tuple[1])
        }
        if (processArgs.mapPassthrough) {
          tuple = tuple.take(2) + processArgs.mapPassthrough(tuple.drop(2))
        }

        // check tuple
        assert tuple instanceof List : 
          "Error in module '${key}': element in channel should be a tuple [id, data, ...otherargs...]\n" +
          "  Example: [\"id\", [input: file('foo.txt'), arg: 10]].\n" +
          "  Expected class: List. Found: tuple.getClass() is ${tuple.getClass()}"
        assert tuple.size() >= 2 : 
          "Error in module '${key}': expected length of tuple in input channel to be two or greater.\n" +
          "  Example: [\"id\", [input: file('foo.txt'), arg: 10]].\n" +
          "  Found: tuple.size() == ${tuple.size()}"
        
        // check id field
        assert tuple[0] instanceof CharSequence : 
          "Error in module '${key}': first element of tuple in channel should be a String\n" +
          "  Example: [\"id\", [input: file('foo.txt'), arg: 10]].\n" +
          "  Found: ${tuple[0]}"
        
        // match file to input file
        if (processArgs.auto.simplifyInput && (tuple[1] instanceof Path || tuple[1] instanceof List)) {
          def inputFiles = thisConfig.functionality.allArguments
            .findAll { it.type == "file" && it.direction == "input" }
          
          assert inputFiles.size() == 1 : 
              "Error in module '${key}' id '${tuple[0]}'.\n" +
              "  Anonymous file inputs are only allowed when the process has exactly one file input.\n" +
              "  Expected: inputFiles.size() == 1. Found: inputFiles.size() is ${inputFiles.size()}"

          tuple[1] = [[ inputFiles[0].plainName, tuple[1] ]].collectEntries()
        }

        // check data field
        assert tuple[1] instanceof Map : 
          "Error in module '${key}' id '${tuple[0]}': second element of tuple in channel should be a Map\n" +
          "  Example: [\"id\", [input: file('foo.txt'), arg: 10]].\n" +
          "  Expected class: Map. Found: tuple[1].getClass() is ${tuple[1].getClass()}"

        // rename keys of data field in tuple
        if (processArgs.renameKeys) {
          assert processArgs.renameKeys instanceof Map : 
              "Error renaming data keys in module '${key}' id '${tuple[0]}'.\n" +
              "  Example: renameKeys: ['new_key': 'old_key'].\n" +
              "  Expected class: Map. Found: renameKeys.getClass() is ${processArgs.renameKeys.getClass()}"
          assert tuple[1] instanceof Map : 
              "Error renaming data keys in module '${key}' id '${tuple[0]}'.\n" +
              "  Expected class: Map. Found: tuple[1].getClass() is ${tuple[1].getClass()}"

          // TODO: allow renameKeys to be a function?
          processArgs.renameKeys.each { newKey, oldKey ->
            assert newKey instanceof CharSequence : 
              "Error renaming data keys in module '${key}' id '${tuple[0]}'.\n" +
              "  Example: renameKeys: ['new_key': 'old_key'].\n" +
              "  Expected class of newKey: String. Found: newKey.getClass() is ${newKey.getClass()}"
            assert oldKey instanceof CharSequence : 
              "Error renaming data keys in module '${key}' id '${tuple[0]}'.\n" +
              "  Example: renameKeys: ['new_key': 'old_key'].\n" +
              "  Expected class of oldKey: String. Found: oldKey.getClass() is ${oldKey.getClass()}"
            assert tuple[1].containsKey(oldKey) : 
              "Error renaming data keys in module '${key}' id '${tuple[0]}'.\n" +
              "  Key '$oldKey' is missing in the data map. tuple[1].keySet() is '${tuple[1].keySet()}'"
            tuple[1].put(newKey, tuple[1][oldKey])
          }
          tuple[1].keySet().removeAll(processArgs.renameKeys.collect{ newKey, oldKey -> oldKey })
        }
        tuple
      }

    if (processArgs.filter) {
      mid2_ = mid1_
        | filter{processArgs.filter(it)}
    } else {
      mid2_ = mid1_
    }

    if (processArgs.fromState) {
      mid3_ = mid2_
        | map{
          def new_data = processArgs.fromState(it.take(2))
          [it[0], new_data]
        }
    } else {
      mid3_ = mid2_
    }

    // fill in defaults
    mid4_ = mid3_
      | map { tuple ->
        def id_ = tuple[0]
        def data_ = tuple[1]

        // TODO: could move fromState to here

        // fetch default params from functionality
        def defaultArgs = thisConfig.functionality.allArguments
          .findAll { it.containsKey("default") }
          .collectEntries { [ it.plainName, it.default ] }

        // fetch overrides in params
        def paramArgs = thisConfig.functionality.allArguments
          .findAll { par ->
            def argKey = key + "__" + par.plainName
            params.containsKey(argKey) && params[argKey] != "viash_no_value"
          }
          .collectEntries { [ it.plainName, params[key + "__" + it.plainName] ] }
        
        // fetch overrides in data
        def dataArgs = thisConfig.functionality.allArguments
          .findAll { data_.containsKey(it.plainName) }
          .collectEntries { [ it.plainName, data_[it.plainName] ] }
        
        // combine params
        def combinedArgs = defaultArgs + paramArgs + processArgs.args + dataArgs

        // remove arguments with explicit null values
        combinedArgs.removeAll{it.value == null}

        if (!workflow.stubRun) {
          // check whether required arguments exist
          thisConfig.functionality.allArguments
            .forEach { par ->
              if (par.required) {
                assert combinedArgs.containsKey(par.plainName) : 
                  "Error in module '${key}' id '${id_}': required argument '${par.plainName}' is missing"
              }

              // TODO: check whether parameters have the right type
            }
        }

        [id_, combinedArgs] + tuple.drop(2)
      }

    out0_ = mid4_
      | _debug(processArgs, "processed")
      | innerWorkflowFactory(processArgs)
      // todo: can we have a postprocessOutputs here?
      | map { id, output ->
        if (processArgs.auto.simplifyOutput && output.size() == 1) {
          output = output.values()[0]
        }
        [id, output]
      }

    // join the output [id, output] with the previous state [id, state, ...]
    out1_ = out0_.join(mid2_, failOnDuplicate: true)
      // input tuple format: [id, output, prev_state, ...]
      // output tuple format: [id, new_state, ...]
      | map{
        def new_state = processArgs.toState(it)
        [it[0], new_state] + it.drop(3)
      }
      | _debug(processArgs, "output")
    
    if (processArgs.auto.publish == "state") {
      out1_
        | publishStates(key: key)
    }

    emit:
    out1_
  }

  def wf = workflowInstance.cloneWithName(workflowKey)

  // add factory function
  wf.metaClass.run = { runArgs ->
    workflowFactory(runArgs)
  }
  // add config to module for later introspection
  wf.metaClass.config = thisConfig

  return wf
}
