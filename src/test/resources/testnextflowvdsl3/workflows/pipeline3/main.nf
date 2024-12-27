nextflow.enable.dsl=2

include { readConfig; helpMessage; channelFromParams } from "${params.rootDir}/workflows/utils/WorkflowHelper.nf"
config = readConfig("${params.rootDir}/workflows/pipeline3/config.vsh.yaml")

workflow base {
  helpMessage(config)

  channelFromParams(params, config)
    | view {"DEBUG: $it"}
}
