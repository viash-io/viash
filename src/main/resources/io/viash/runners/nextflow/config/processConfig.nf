def processConfig(config) {
  // TODO: assert .functionality etc.
  if (config.functionality.inputs) {
    System.err.println("Warning: .functionality.inputs is deprecated. Please use .functionality.arguments instead.")
  }
  if (config.functionality.outputs) {
    System.err.println("Warning: .functionality.outputs is deprecated. Please use .functionality.arguments instead.")
  }

  // set defaults for inputs
  config.functionality.inputs = 
    (config.functionality.inputs != null ? config.functionality.inputs : []).collect{arg ->
      arg.type = arg.type != null ? arg.type : "file"
      arg.direction = "input"
      _processArgument(arg)
    }
  // set defaults for outputs
  config.functionality.outputs = 
    (config.functionality.outputs != null ? config.functionality.outputs : []).collect{arg ->
      arg.type = arg.type != null ? arg.type : "file"
      arg.direction = "output"
      _processArgument(arg)
    }
  // set defaults for arguments
  config.functionality.arguments = 
    (config.functionality.arguments != null ? config.functionality.arguments : []).collect{arg ->
      _processArgument(arg)
    }
  // set defaults for argument_group arguments
  config.functionality.argument_groups =
    (config.functionality.argument_groups != null ? config.functionality.argument_groups : []).collect{grp ->
      grp.arguments = (grp.arguments != null ? grp.arguments : []).collect{arg ->
        arg instanceof String ? arg.replaceAll("^-*", "") : _processArgument(arg)
      }
      grp
    }

  // create combined arguments list
  config.functionality.allArguments = 
    config.functionality.inputs +
    config.functionality.outputs +
    config.functionality.arguments +
    config.functionality.argument_groups.collectMany{ group ->
      group.arguments.findAll{ it !instanceof String }
    }
  
  // add missing argument groups (based on Functionality::allArgumentGroups())
  def argGroups = config.functionality.argument_groups
  def inputGroup = _processArgumentGroup(argGroups, "Inputs", config.functionality.inputs)
  def outputGroup = _processArgumentGroup(argGroups, "Outputs", config.functionality.outputs)
  def defaultGroup = _processArgumentGroup(argGroups, "Arguments", config.functionality.arguments)
  def groupsFiltered = argGroups.findAll(gr -> !(["Inputs", "Outputs", "Arguments"].contains(gr.name)))
  config.functionality.allArgumentGroups = inputGroup + outputGroup + defaultGroup + groupsFiltered

  config
}
