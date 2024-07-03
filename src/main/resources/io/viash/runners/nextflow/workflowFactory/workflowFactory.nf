def _debug(workflowArgs, debugKey) {
  if (workflowArgs.debug) {
    view { "process '${workflowArgs.key}' $debugKey tuple: $it"  }
  } else {
    map { it }
  }
}

// depends on: innerWorkflowFactory
def workflowFactory(Map args, Map defaultWfArgs, Map meta) {
  def workflowArgs = processWorkflowArgs(args, defaultWfArgs, meta)
  def key_ = workflowArgs["key"]
  
  workflow workflowInstance {
    take: input_

    main:
    def chModified = input_
      | checkUniqueIds([:])
      | _debug(workflowArgs, "input")
      | map { tuple ->
        tuple = deepClone(tuple)
        
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
        if (tuple[0] instanceof GString) {
          tuple[0] = tuple[0].toString()
        }
        assert tuple[0] instanceof CharSequence : 
          "Error in module '${key_}': first element of tuple in channel should be a String\n" +
          "  Example: [\"id\", [input: file('foo.txt'), arg: 10]].\n" +
          "  Found: ${tuple[0]}"
        
        // match file to input file
        if (workflowArgs.auto.simplifyInput && (tuple[1] instanceof Path || tuple[1] instanceof List)) {
          def inputFiles = meta.config.allArguments
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

    def chModifiedFiltered = workflowArgs.filter ?
      chModified | filter{workflowArgs.filter(it)} :
      chModified

    def chRun = null
    def chPassthrough = null
    if (workflowArgs.runIf) {
      def runIfBranch = chModifiedFiltered.branch{ tup ->
        run: workflowArgs.runIf(tup[0], tup[1])
        passthrough: true
      }
      chRun = runIfBranch.run
      chPassthrough = runIfBranch.passthrough
    } else {
      chRun = chModifiedFiltered
      chPassthrough = Channel.empty()
    }

    def chArgs = workflowArgs.fromState ? 
      chRun | map{
        def new_data = workflowArgs.fromState(it.take(2))
        [it[0], new_data]
      } :
      chRun | map {tup -> tup.take(2)}

    // fill in defaults
    def chArgsWithDefaults = chArgs
      | map { tuple ->
        def id_ = tuple[0]
        def data_ = tuple[1]

        // TODO: could move fromState to here

        // fetch default params from functionality
        def defaultArgs = meta.config.allArguments
          .findAll { it.containsKey("default") }
          .collectEntries { [ it.plainName, it.default ] }

        // fetch overrides in params
        def paramArgs = meta.config.allArguments
          .findAll { par ->
            def argKey = key_ + "__" + par.plainName
            params.containsKey(argKey)
          }
          .collectEntries { [ it.plainName, params[key_ + "__" + it.plainName] ] }
        
        // fetch overrides in data
        def dataArgs = meta.config.allArguments
          .findAll { data_.containsKey(it.plainName) }
          .collectEntries { [ it.plainName, data_[it.plainName] ] }
        
        // combine params
        def combinedArgs = defaultArgs + paramArgs + workflowArgs.args + dataArgs

        // remove arguments with explicit null values
        combinedArgs
          .removeAll{_, val -> val == null || val == "viash_no_value" || val == "force_null"}

        combinedArgs = _processInputValues(combinedArgs, meta.config, id_, key_)

        [id_, combinedArgs] + tuple.drop(2)
      }

    // TODO: move some of the _meta.join_id wrangling to the safeJoin() function.
    def chInitialOutputMulti = chArgsWithDefaults
      | _debug(workflowArgs, "processed")
      // run workflow
      | innerWorkflowFactory(workflowArgs)
    def chInitialOutputListPossibleNested = [chInitialOutputMulti]
    def chInitialOutputList = chInitialOutputMulti instanceof List ? chInitialOutputMulti : chInitialOutputListPossibleNested
    assert chInitialOutputMulti.size() > 0: "${key_}: should have emitted at least one output channel"
    def chInitialOutput = chInitialOutputList > 1 ? chInitialOutputList[0].mix(*chInitialOutputList.tail()) : chInitialOutputList 
    def chInitialOutputProcessed = chInitialOutput
      | map { id_, output_ ->

        // see if output map contains metadata
        def meta_ =
          output_ instanceof Map && output_.containsKey("_meta") ? 
          output_["_meta"] :
          [:]
        def join_id = meta_.join_id ?: id_
        
        // remove metadata
        output_ = output_.findAll{k, v -> k != "_meta"}

        // check value types
        output_ = _checkValidOutputArgument(output_, meta.config, id_, key_)

        // simplify output if need be
        if (workflowArgs.auto.simplifyOutput && output_.size() == 1) {
          output_ = output_.values()[0]
        }

        [join_id, id_, output_]
      }
      // | view{"chInitialOutput: ${it.take(3)}"}

    // join the output [prev_id, new_id, output] with the previous state [prev_id, state, ...]
    def chNewState = safeJoin(chInitialOutputProcessed, chModifiedFiltered, key_)
      // input tuple format: [join_id, id, output, prev_state, ...]
      // output tuple format: [join_id, id, new_state, ...]
      | map{ tup ->
        def new_state = workflowArgs.toState(tup.drop(1).take(3))
        tup.take(2) + [new_state] + tup.drop(4)
      }

    def chJoined = chNewState
      | groupTuple(by: 1, sort: 'hash', size: chInitialOutputMulti.size(), remainder: true)
      | map {join_ids, id, states ->
        def newJoinId = join_ids.unique{a, b -> a <=> b}
        assert newJoinId.size() == 1: "Multiple join IDs were emitted for '$id'."
        newJoinId = newJoinId[0]
        def newState = states.inject([:]){ old_state, state_to_add ->
          def overlap = old_state.keySet().intersect(state_to_add.keySet())
          assert overlap.isEmpty() : "ID $id: multiple entries for for argument(s) $overlap were emitted."
          def return_state = old_state + state_to_add
          return return_state 
        }
        return [newJoinId, id, newState]
      }
    
    if (workflowArgs.auto.publish == "state") {
      def chPublishStates = chJoined
        // input tuple format: [join_id, id, new_state, ...]
        // output tuple format: [join_id, id, new_state]
        | map{ tup ->
          tup.take(3)
        }

      safeJoin(chPublishStates, chArgsWithDefaults, key_)
        // input tuple format: [join_id, id, new_state, orig_state, ...]
        // output tuple format: [id, new_state, orig_state]
        | map { tup ->
          tup.drop(1).take(3)
        }
        | publishStatesByConfig(key: key_, config: meta.config)
    }
    def chReturn = chPublishStates
      | map { tup ->
        // input tuple format: [join_id, id, new_state, ...]
        // output tuple format: [id, new_state, ...]
        tup.drop(1)
      }
      | _debug(workflowArgs, "output")
      | concat(chPassthrough)

    emit: chReturn
  }

  def wf = workflowInstance.cloneWithName(key_)

  // add factory function
  wf.metaClass.run = { runArgs ->
    workflowFactory(runArgs, workflowArgs, meta)
  }
  // add config to module for later introspection
  wf.metaClass.config = meta.config

  return wf
}
