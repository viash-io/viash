nextflow.enable.dsl=2

include { readConfig; helpMessage; preprocessInputs; channelFromParams } from "${params.rootDir}/workflows/utils/WorkflowHelper.nf"
config = readConfig("${params.rootDir}/workflows/pipeline3/config.vsh.yaml")

workflow base {
  helpMessage(config)

  channelFromParams(params, config)
    | preprocessInputs(["config": config])
    | view {"DEBUG: $it"}
}
