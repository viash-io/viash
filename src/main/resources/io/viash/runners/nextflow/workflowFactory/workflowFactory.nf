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
  def multipleArgs = meta.config.allArguments.findAll{ it.multiple }.collect{it.plainName}

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


    def chRun = null
    def chPassthrough = null
    if (workflowArgs.runIf) {
      def runIfBranch = chModified.branch{ tup ->
        run: workflowArgs.runIf(tup[0], tup[1])
        passthrough: true
      }
      chRun = runIfBranch.run
      chPassthrough = runIfBranch.passthrough
    } else {
      chRun = chModified
      chPassthrough = Channel.empty()
    }

    def chPassthroughElse = null
    if (runElse_){
      chPassthroughElse = chPassthrough.map{tup ->
        runElse_(tup[0], tup[1])
      }
    } else {
      chPassthroughElse = Channel.empty() 
    }

    def chRunFiltered = workflowArgs.filter ?
      chRun | filter{workflowArgs.filter(it)} :
      chRun

    def chArgs = workflowArgs.fromState ? 
      chRunFiltered | map{
        def new_data = workflowArgs.fromState(it.take(2))
        [it[0], new_data]
      } :
      chRunFiltered | map {tup -> tup.take(2)}

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
    def chInitialOutputList = chInitialOutputMulti instanceof List ? chInitialOutputMulti : [chInitialOutputMulti]
    assert chInitialOutputList.size() > 0: "should have emitted at least one output channel"
    // Add a channel ID to the events, which designates the channel the event was emitted from as a running number
    // This number is used to sort the events later when the events are gathered from across the channels.
    def chInitialOutputListWithIndexedEvents = chInitialOutputList.withIndex().collect{channel, channelIndex ->
      def newChannel = channel
        | map {tuple ->
          assert tuple instanceof List : 
          "Error in module '${key_}': element in output channel should be a tuple [id, data, ...otherargs...]\n" +
          "  Example: [\"id\", [input: file('foo.txt'), arg: 10]].\n" +
          "  Expected class: List. Found: tuple.getClass() is ${tuple.getClass()}"
        
          def newEvent = [channelIndex] + tuple
          return newEvent
        }
      return newChannel
    }
    // Put the events into 1 channel, cover case where there is only one channel is emitted
    def chInitialOutput = chInitialOutputList.size() > 1 ? \
      chInitialOutputListWithIndexedEvents[0].mix(*chInitialOutputListWithIndexedEvents.tail()) : \
      chInitialOutputListWithIndexedEvents[0]
    def chInitialOutputProcessed = chInitialOutput
      | map { tuple  ->
        def channelId = tuple[0]
        def id_ = tuple[1]
        def output_ = tuple[2]

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

        [join_id, channelId, id_, output_]
      }
      // | view{"chInitialOutput: ${it.take(3)}"}

    // join the output [prev_id, channel_id, new_id, output] with the previous state [prev_id, state, ...]
    def chPublishWithPreviousState = safeJoin(chInitialOutputProcessed, chRunFiltered, key_)
      // input tuple format: [join_id, channel_id, id, output, prev_state, ...]
      // output tuple format: [join_id, channel_id, id, new_state, ...]
      | map{ tup ->
        def new_state = workflowArgs.toState(tup.drop(2).take(3))
        tup.take(3) + [new_state] + tup.drop(5)
      }
    if (workflowArgs.auto.publish == "state") {
      def chPublishFiles = chPublishWithPreviousState
        // input tuple format: [join_id, channel_id, id, new_state, ...]
        // output tuple format: [join_id, channel_id, id, new_state]
        | map{ tup ->
          tup.take(4)
        }

      safeJoin(chPublishFiles, chArgsWithDefaults, key_)
        // input tuple format: [join_id, channel_id, id, new_state, orig_state, ...]
        // output tuple format: [id, new_state, orig_state]
        | map { tup ->
          tup.drop(2).take(3)
        }
        | publishFilesByConfig(key: key_, config: meta.config)
    }
    // Join the state from the events that were emitted from different channels
    def chJoined = chInitialOutputProcessed
      | map {tuple ->
        def join_id = tuple[0]
        def channel_id = tuple[1]
        def id = tuple[2]
        def other = tuple.drop(3)
        // Below, groupTuple is used to join the events. To make sure resuming a workflow
        // keeps working, the output state must be deterministic. This means the state needs to be
        // sorted with groupTuple's has a 'sort' argument. This argument can be set to 'hash',
        // but hashing the state when it is large can be problematic in terms of performance.
        // Therefore, a custom comparator function is provided. We add the channel ID to the 
        // states so that we can use the channel ID to sort the items. 
        def stateWithChannelID = [[channel_id] * other.size(), other].transpose()
        // A comparator that is provided to groupTuple's 'sort' argument is applied
        // to all elements of the event tuple (that is not the 'id'). The comparator
        // closure that is used below expects the input to be List. So the join_id and
        // channel_id must also be wrapped in a list. 
        [[join_id], [channel_id], id] + stateWithChannelID
      }
      | groupTuple(by: 2, sort: {a, b -> a[0] <=> b[0]}, size: chInitialOutputList.size(), remainder: true)
      | map {join_ids, _, id, statesWithChannelID ->
        // Remove the channel IDs from the states
        def states = statesWithChannelID.collect{it[1]}
        def newJoinId = join_ids.flatten().unique{a, b -> a <=> b}
        assert newJoinId.size() == 1: "Multiple events were emitted for '$id'."
        def newJoinIdUnique = newJoinId[0]
        def newState = states.inject([:]){ old_state, state_to_add ->
          def stateToAddNoMultiple = state_to_add.findAll{k, v -> !multipleArgs.contains(k)}
          // First add non multiple arguments

          def overlap = old_state.keySet().intersect(stateToAddNoMultiple.keySet())
          assert overlap.isEmpty() : "ID $id: multiple entries for " + 
            " argument(s) $overlap were emitted."
          def return_state = old_state + stateToAddNoMultiple

          // Add `multiple: true` arguments
          def stateToAddMultiple = state_to_add.findAll{k, v -> multipleArgs.contains(k)}
          stateToAddMultiple.each {k, v ->
            def currentKey = return_state.getOrDefault(k, [])
            def currentKeyList = currentKey instanceof List ? currentKey : [currentKey]
            currentKeyList.add(v)
            return_state[k] = currentKeyList
          }
          return return_state
        }

        _checkAllRequiredOuputsPresent(newState, meta.config, id, key_)

        // simplify output if need be
        if (workflowArgs.auto.simplifyOutput && newState.size() == 1) {
          newState = newState.values()[0]
        }

        return [newJoinIdUnique, id, newState]
      }
    
    // join the output [prev_id, new_id, output] with the previous state [prev_id, state, ...]
    def chNewState = safeJoin(chJoined, chRunFiltered, key_)
      // input tuple format: [join_id, id, output, prev_state, ...]
      // output tuple format: [join_id, id, new_state, ...]
      | map{ tup ->
        def new_state = workflowArgs.toState(tup.drop(1).take(3))
        tup.take(2) + [new_state] + tup.drop(4)
      }

    if (workflowArgs.auto.publish == "state") {
      def chPublishStates = chNewState
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
    chReturn = chNewState
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
