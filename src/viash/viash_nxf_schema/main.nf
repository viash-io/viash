nextflow.enable.dsl=2

include { readConfig; addGlobalParams } from params.resourcesDir + "/WorkflowHelper.nf"

def config = readConfig(params.input)
def mergedConfig = addGlobalParams(config)

import groovy.json.JsonOutput

def json_str = JsonOutput.toJson(mergedConfig)
def json_beauty = JsonOutput.prettyPrint(json_str)
File file = new File(params.output)
file.write(json_beauty)