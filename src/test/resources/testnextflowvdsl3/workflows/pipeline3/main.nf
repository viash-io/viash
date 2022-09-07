nextflow.enable.dsl=2

include { readConfig; viashChannel; helpMessage } from "${params.rootDir}/workflows/utils/WorkflowHelper.nf"
config = readConfig("${params.rootDir}/workflows/pipeline3/config.vsh.yaml")

workflow base {
  helpMessage(config)

  viashChannel(params, config)
    | view{"DEBUG: $it"}
}
