def findStates(Map params, Map config) {
  // TODO: do a deep clone of config
  def auto_config = deepClone(config)
  def auto_params = deepClone(params)

  auto_config = auto_config.clone()
  // override arguments
  auto_config.functionality.argument_groups = []
  auto_config.functionality.arguments = [
    [
      type: "string",
      name: "--id",
      description: "A dummy identifier",
      required: false
    ],
    [
      type: "file",
      name: "--input_states",
      example: "/path/to/input/directory/**/state.yaml",
      description: "Path to input directory containing the datasets to be integrated.",
      required: true,
      multiple: true,
      multiple_sep: ";"
    ],
    [
      type: "string",
      name: "--filter",
      example: "foo/.*/state.yaml",
      description: "Regex to filter state files by path.",
      required: false
    ],
    // to do: make this a yaml blob?
    [
      type: "string",
      name: "--rename_keys",
      example: ["newKey1:oldKey1", "newKey2:oldKey2"],
      description: "Rename keys in the detected input files. This is useful if the input files do not match the set of input arguments of the workflow.",
      required: false,
      multiple: true,
      multiple_sep: ";"
    ],
    [
      type: "string",
      name: "--settings",
      example: '{"output_dataset": "dataset.h5ad", "k": 10}',
      description: "Global arguments as a JSON glob to be passed to all components.",
      required: false
    ]
  ]
  if (!(auto_params.containsKey("id"))) {
    auto_params["id"] = "auto"
  }

  // run auto config through processConfig once more
  auto_config = processConfig(auto_config)

  workflow findStatesWf {
    helpMessage(auto_config)

    output_ch = 
      channelFromParams(auto_params, auto_config)
        | flatMap { autoId, args ->

          def globalSettings = args.settings ? readYamlBlob(args.settings) : [:]

          // look for state files in input dir
          def stateFiles = args.input_states

          // filter state files by regex
          if (args.filter) {
            stateFiles = stateFiles.findAll{ stateFile ->
              def stateFileStr = stateFile.toString()
              def matcher = stateFileStr =~ args.filter
              matcher.matches()}
          }

          // read in states
          def states = stateFiles.collect { stateFile ->
            def state_ = readTaggedYaml(stateFile)
            [state_.id, state_]
          }

          // construct renameMap
          if (args.rename_keys) {
            def renameMap = args.rename_keys.collectEntries{renameString ->
              def split = renameString.split(":")
              assert split.size() == 2: "Argument 'rename_keys' should be of the form 'newKey:oldKey,newKey:oldKey'"
              split
            }

            // rename keys in state, only let states through which have all keys
            // also add global settings
            states = states.collectMany{id, state ->
              def newState = [:]

              for (key in renameMap.keySet()) {
                def origKey = renameMap[key]
                if (!(state.containsKey(origKey))) {
                  return []
                }
                newState[key] = state[origKey]
              }

              [[id, globalSettings + newState]]
            }
          }

          states
        }
    emit:
    output_ch
  }

  return findStatesWf
}
