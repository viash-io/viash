// depends on: thisConfig, params, resourcesDir
// TODO: do the defaultArgs, paramArgs and args.args need to be merged somewhere else?
def vdsl3RunWorkflowFactory(Map args) {
  def key = args["key"]
  def processObj = null

  workflow processWf {
    take: input_
    main:

    if (processObj == null) {
      processObj = processFactory(args)
    }
    
    output_ = input_
      | map { tuple ->
        def id = tuple[0]
        def data = tuple[1]

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
          .findAll { data.containsKey(it.plainName) }
          .collectEntries { [ it.plainName, data[it.plainName] ] }
        
        // combine params
        def combinedArgs = defaultArgs + paramArgs + args.args + dataArgs

        // remove arguments with explicit null values
        combinedArgs.removeAll{it.value == null}

        if (workflow.stubRun) {
          // add id if missing
          combinedArgs = [id: 'stub'] + combinedArgs
        } else {
          // check whether required arguments exist
          thisConfig.functionality.allArguments
            .forEach { par ->
              if (par.required) {
                assert combinedArgs.containsKey(par.plainName): "Argument ${par.plainName} is required but does not have a value"
              }
            }
        }

        // TODO: check whether parameters have the right type

        // process input files separately
        def inputPaths = thisConfig.functionality.allArguments
          .findAll { it.type == "file" && it.direction == "input" }
          .collect { par ->
            def val = combinedArgs.containsKey(par.plainName) ? combinedArgs[par.plainName] : []
            def inputFiles = []
            if (val == null) {
              inputFiles = []
            } else if (val instanceof List) {
              inputFiles = val
            } else if (val instanceof Path) {
              inputFiles = [ val ]
            } else {
              inputFiles = []
            }
            if (!workflow.stubRun) {
              // throw error when an input file doesn't exist
              inputFiles.each{ file -> 
                assert file.exists() :
                  "Error in module '${key}' id '${id}' argument '${par.plainName}'.\n" +
                  "  Required input file does not exist.\n" +
                  "  Path: '$file'.\n" +
                  "  Expected input file to exist"
              }
            }
            inputFiles 
          } 

        // remove input files
        def argsExclInputFiles = thisConfig.functionality.allArguments
          .findAll { (it.type != "file" || it.direction != "input") && combinedArgs.containsKey(it.plainName) }
          .collectEntries { par ->
            def parName = par.plainName
            def val = combinedArgs[parName]
            if (par.multiple && val instanceof Collection) {
              val = val.join(par.multiple_sep)
            }
            if (par.direction == "output" && par.type == "file") {
              val = val.replaceAll('\\$id', id).replaceAll('\\$key', key)
            }
            [parName, val]
          }

        [ id ] + inputPaths + [ argsExclInputFiles, resourcesDir ]
      }
      | processObj
      | map { output ->
        def outputFiles = thisConfig.functionality.allArguments
          .findAll { it.type == "file" && it.direction == "output" }
          .indexed()
          .collectEntries{ index, par ->
            out = output[index + 1]
            // strip dummy '.exitcode' file from output (see nextflow-io/nextflow#2678)
            if (!out instanceof List || out.size() <= 1) {
              if (par.multiple) {
                out = []
              } else {
                assert !par.required :
                    "Error in module '${key}' id '${output[0]}' argument '${par.plainName}'.\n" +
                    "  Required output file is missing"
                out = null
              }
            } else if (out.size() == 2 && !par.multiple) {
              out = out[1]
            } else {
              out = out.drop(1)
            }
            [ par.plainName, out ]
          }
        
        // drop null outputs
        outputFiles.removeAll{it.value == null}

        [ output[0], outputFiles ]
      }
    emit: output_
  }

  return processWf
}
