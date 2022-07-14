nextflow.enable.dsl=2

include { readConfig; addGlobalParams } from params.resourcesDir + "/WorkflowHelper.nf"

def config = readConfig(params.input)

def text = config.functionality.argument_groups.collect{group -> 
  def argText = group.arguments.collect{argName ->
    def arg = config.functionality.allArguments.find{it.plainName == argName}

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
    quo = (arg.type == "string" || arg.type == "file") ? "\"" : ""
    "${arg.plainName}: $quo$val$quo\n"
  }.join()

  "# ${group.name}\n${argText}"
}.join("\n")

println(text)