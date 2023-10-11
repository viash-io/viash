def processConfig(config) {
  // set defaults for arguments
  config.functionality.arguments = 
    (config.functionality.arguments ?: []).collect{_processArgument(it)}

  // set defaults for argument_group arguments
  config.functionality.argument_groups =
    (config.functionality.argument_groups ?: []).collect{grp ->
      grp.arguments = (grp.arguments ?: []).collect{_processArgument(it)}
      grp
    }

  // create combined arguments list
  config.functionality.allArguments = 
    config.functionality.arguments +
    config.functionality.argument_groups.collectMany{it.arguments}

  // add missing argument groups (based on Functionality::allArgumentGroups())
  def argGroups = config.functionality.argument_groups
  if (argGroups.any{it.name.toLowerCase() == "arguments"}) {
    argGroups = argGroups.collect{ grp ->
      if (grp.name.toLowerCase() == "arguments") {
        grp = grp + [
          arguments: grp.arguments + config.functionality.arguments
        ]
      }
      grp
    }
  } else {
    argGroups = argGroups + [
      name: "Arguments",
      arguments: config.functionality.arguments
    ]
  }
  config.functionality.allArgumentGroups = argGroups

  config
}
