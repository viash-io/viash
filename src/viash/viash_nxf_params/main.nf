nextflow.enable.dsl=2

include { readConfig; addGlobalParams } from params.resourcesDir + "/WorkflowHelper.nf"

def config = readConfig(params.input)

// add nxf global params
if (params.addGlobals) {
  config = addGlobalParams(config)
}

def text = config.functionality.argument_groups.collect{group -> 
  def argText = group.arguments.collect{argName ->
    def arg = config.functionality.allArguments.find{it.plainName == argName}
    
    // set output files to optional
    if (arg.type == "file" && arg.direction == "output") {
      arg.required = false
      arg.example = arg.default ?: arg.example
      arg.default = null
    }

    def name = arg.plainName
    def val = ""
    // based on DebugPlatform.scala
    if (arg.default != null) {
        val = arg.default
    } else if (arg.default == null && arg.example != null) {
        val = arg.example
    } else if (arg.type == "boolean" || arg.type == "boolean_true" || arg.type == "false") {
        val = "true"
    } else if (arg.type == "double") {
        val = "123.0"
    } else if (arg.type == "integer") {
        val = "123"
    } else if (arg.type == "string") {
        val = "foo"
    } else if (arg.type == "file") {
        val = "path/to/file"
    }
    // split multiple if need be
    if (arg.multiple && val !instanceof List) {
      val = val.split(arg.multiple_sep).toList()
    }
    // enquote values
    if (arg.type == "string" || arg.type == "file") {
      if (arg.multiple && val !instanceof String) {
        val = val.collect{"\"$it\""}
      } else {
        val = "\"$val\""
      }
    }
    if (arg.required) {
      val = "# please fill in - example: $val"
    } else if (arg.default == null) {
      name = "# $name"
    }

    "$name: $val\n"
  }.join()

  "# ${group.name}\n${argText}"
}.join("\n")

println(text)