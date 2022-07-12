nextflow.enable.dsl=2

params.rootDir = "${projectDir}/../../"
params.config = "${params.rootDir}/workflows/pipeline3/config.vsh.yaml"

include { readConfig; viashChannel; helpMessage } from "${params.rootDir}/workflows/utils/WorkflowHelper.nf"

config = readConfig(params.config)

helpMessage(config)
