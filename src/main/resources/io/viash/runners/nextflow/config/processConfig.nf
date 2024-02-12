def processConfig(config) {
  // set defaults for arguments
  config.arguments = 
    (config.arguments ?: []).collect{_processArgument(it)}

  // set defaults for argument_group arguments
  config.argument_groups =
    (config.argument_groups ?: []).collect{grp ->
      grp.arguments = (grp.arguments ?: []).collect{_processArgument(it)}
      grp
    }

  // create combined arguments list
  config.allArguments = 
    config.arguments +
    config.argument_groups.collectMany{it.arguments}

  // add missing argument groups (based on Functionality::allArgumentGroups())
  def argGroups = config.argument_groups
  if (argGroups.any{it.name.toLowerCase() == "arguments"}) {
    argGroups = argGroups.collect{ grp ->
      if (grp.name.toLowerCase() == "arguments") {
        grp = grp + [
          arguments: grp.arguments + config.arguments
        ]
      }
      grp
    }
  } else {
    argGroups = argGroups + [
      name: "Arguments",
      arguments: config.arguments
    ]
  }
  config.allArgumentGroups = argGroups

  config
}
