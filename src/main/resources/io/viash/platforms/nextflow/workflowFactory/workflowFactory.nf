def _debug(workflowArgs, debugKey) {
  if (workflowArgs.debug) {
    view { "process '${workflowArgs.key}' $debugKey tuple: $it"  }
  } else {
    map { it }
  }
}

// depends on: thisConfig, innerWorkflowFactory
def workflowFactory(Map args) {
  def workflowArgs = processWorkflowArgs(args)
  def key_ = workflowArgs["key"]
  
  workflow workflowInstance {
    take: input_

    main:
    mid1_ = input_
      | checkUniqueIds([:])
      | _debug(workflowArgs, "input")
      | map { tuple ->
        tuple = tuple.clone()
        
        if (workflowArgs.map) {
          tuple = workflowArgs.map(tuple)
        }
        if (workflowArgs.mapId) {
          tuple[0] = workflowArgs.mapId(tuple[0])
        }
        if (workflowArgs.mapData) {
          tuple[1] = workflowArgs.mapData(tuple[1])
        }
        if (workflowArgs.mapPassthrough) {
          tuple = tuple.take(2) + workflowArgs.mapPassthrough(tuple.drop(2))
        }

        // check tuple
        assert tuple instanceof List : 
          "Error in module '${key_}': element in channel should be a tuple [id, data, ...otherargs...]\n" +
          "  Example: [\"id\", [input: file('foo.txt'), arg: 10]].\n" +
          "  Expected class: List. Found: tuple.getClass() is ${tuple.getClass()}"
        assert tuple.size() >= 2 : 
          "Error in module '${key_}': expected length of tuple in input channel to be two or greater.\n" +
          "  Example: [\"id\", [input: file('foo.txt'), arg: 10]].\n" +
          "  Found: tuple.size() == ${tuple.size()}"
        
        // check id field
        assert tuple[0] instanceof CharSequence : 
          "Error in module '${key_}': first element of tuple in channel should be a String\n" +
          "  Example: [\"id\", [input: file('foo.txt'), arg: 10]].\n" +
          "  Found: ${tuple[0]}"
        
        // match file to input file
        if (workflowArgs.auto.simplifyInput && (tuple[1] instanceof Path || tuple[1] instanceof List)) {
          def inputFiles = thisConfig.functionality.allArguments
            .findAll { it.type == "file" && it.direction == "input" }
          
          assert inputFiles.size() == 1 : 
              "Error in module '${key_}' id '${tuple[0]}'.\n" +
              "  Anonymous file inputs are only allowed when the process has exactly one file input.\n" +
              "  Expected: inputFiles.size() == 1. Found: inputFiles.size() is ${inputFiles.size()}"

          tuple[1] = [[ inputFiles[0].plainName, tuple[1] ]].collectEntries()
        }

        // check data field
        assert tuple[1] instanceof Map : 
          "Error in module '${key_}' id '${tuple[0]}': second element of tuple in channel should be a Map\n" +
          "  Example: [\"id\", [input: file('foo.txt'), arg: 10]].\n" +
          "  Expected class: Map. Found: tuple[1].getClass() is ${tuple[1].getClass()}"

        // rename keys of data field in tuple
        if (workflowArgs.renameKeys) {
          assert workflowArgs.renameKeys instanceof Map : 
              "Error renaming data keys in module '${key_}' id '${tuple[0]}'.\n" +
              "  Example: renameKeys: ['new_key': 'old_key'].\n" +
              "  Expected class: Map. Found: renameKeys.getClass() is ${workflowArgs.renameKeys.getClass()}"
          assert tuple[1] instanceof Map : 
              "Error renaming data keys in module '${key_}' id '${tuple[0]}'.\n" +
              "  Expected class: Map. Found: tuple[1].getClass() is ${tuple[1].getClass()}"

          // TODO: allow renameKeys to be a function?
          workflowArgs.renameKeys.each { newKey, oldKey ->
            assert newKey instanceof CharSequence : 
              "Error renaming data keys in module '${key_}' id '${tuple[0]}'.\n" +
              "  Example: renameKeys: ['new_key': 'old_key'].\n" +
              "  Expected class of newKey: String. Found: newKey.getClass() is ${newKey.getClass()}"
            assert oldKey instanceof CharSequence : 
              "Error renaming data keys in module '${key_}' id '${tuple[0]}'.\n" +
              "  Example: renameKeys: ['new_key': 'old_key'].\n" +
              "  Expected class of oldKey: String. Found: oldKey.getClass() is ${oldKey.getClass()}"
            assert tuple[1].containsKey(oldKey) : 
              "Error renaming data keys in module '${key}' id '${tuple[0]}'.\n" +
              "  Key '$oldKey' is missing in the data map. tuple[1].keySet() is '${tuple[1].keySet()}'"
            tuple[1].put(newKey, tuple[1][oldKey])
          }
          tuple[1].keySet().removeAll(workflowArgs.renameKeys.collect{ newKey, oldKey -> oldKey })
        }
        tuple
      }

    if (workflowArgs.filter) {
      mid2_ = mid1_
        | filter{workflowArgs.filter(it)}
    } else {
      mid2_ = mid1_
    }

    if (workflowArgs.fromState) {
      mid3_ = mid2_
        | map{
          def new_data = workflowArgs.fromState(it.take(2))
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
            def argKey = key_ + "__" + par.plainName
            params.containsKey(argKey)
          }
          .collectEntries { [ it.plainName, params[key_ + "__" + it.plainName] ] }
        
        // fetch overrides in data
        def dataArgs = thisConfig.functionality.allArguments
          .findAll { data_.containsKey(it.plainName) }
          .collectEntries { [ it.plainName, data_[it.plainName] ] }
        
        // combine params
        def combinedArgs = defaultArgs + paramArgs + workflowArgs.args + dataArgs

        // remove arguments with explicit null values
        combinedArgs
          .removeAll{_, val -> val == null || val == "viash_no_value" || val == "force_null"}

        combinedArgs = processInputs(combinedArgs, thisConfig, id_, key_)

        [id_, combinedArgs] + tuple.drop(2)
      }

    // TODO: move some of the _meta.join_id wrangling to the safeJoin() function.

    out0_ = mid4_
      | _debug(workflowArgs, "processed")
      // run workflow
      | innerWorkflowFactory(workflowArgs)
      // check output tuple
      | map { id_, output_ ->

        // see if output map contains metadata
        def meta_ =
          output_ instanceof Map && output_.containsKey("_meta") ? 
          output_["_meta"] :
          [:]
        if (!meta_.containsKey("join_id")) {
          meta_ = meta_ + ["join_id": id_]
        }
        
        // remove metadata
        output_ = output_.findAll{k, v -> k != "_meta"}

        output_ = processOutputs(output_, thisConfig, id_, key_)

        if (workflowArgs.auto.simplifyOutput && output_.size() == 1) {
          output_ = output_.values()[0]
        }

        [meta_.join_id, meta_, id_, output_]
      }
      // | view{"out0_: ${it.take(3)}"}

    // TODO: this join will fail if the keys changed during the innerWorkflowFactory
    // join the output [join_id, meta, id, output] with the previous state [id, state, ...]
    out1_ = safeJoin(out0_, mid2_, key_)
      // input tuple format: [join_id, meta, id, output, prev_state, ...]
      // output tuple format: [join_id, meta, id, new_state, ...]
      | map{ tup ->
        def new_state = workflowArgs.toState(tup.drop(2).take(3))
        tup.take(3) + [new_state] + tup.drop(5)
      }

    if (workflowArgs.auto.publish == "state") {
      out1pub_ = out1_
        // input tuple format: [join_id, meta, id, new_state, ...]
        // output tuple format: [join_id, meta, id, new_state]
        | map{ tup ->
          tup.take(4)
        }

      safeJoin(out1pub_, mid4_, key_)
        // input tuple format: [join_id, meta, id, new_state, orig_state, ...]
        // output tuple format: [id, new_state, orig_state]
        | map { tup ->
          tup.drop(2).take(3)
      }
        | publishStatesByConfig(key: key_, config: thisConfig)
    }

    // remove join_id and meta
    out2_ = out1_
      | map { tup ->
        // input tuple format: [join_id, meta, id, new_state, ...]
        // output tuple format: [id, new_state, ...]
        tup.drop(2)
      }
      | _debug(workflowArgs, "output")

    out2_

    emit: out2_
  }

  def wf = workflowInstance.cloneWithName(key_)

  // add factory function
  wf.metaClass.run = { runArgs ->
    workflowFactory(runArgs)
  }
  // add config to module for later introspection
  wf.metaClass.config = thisConfig

  return wf
}
