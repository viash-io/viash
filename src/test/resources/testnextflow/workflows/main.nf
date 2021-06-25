nextflow.enable.dsl=2

targetDir = "${params.rootDir}/target/nextflow"

include  { step1 }    from "$targetDir/step1/main.nf"       params(params)
include  { step2 }    from "$targetDir/step2/main.nf"       params(params)
include  { step3 }    from "$targetDir/step3/main.nf"       params(params)

workflow {
    second = Channel.fromPath("${params.rootDir}/resources/lines3.txt")
    
    first = Channel.fromPath("${params.rootDir}/resources/lines*.txt") \
        | toList() \
        | map{ [ "foo", [ "input" : it ], params ] } \
        | step1 \
        | view { ["DEBUG1", it[0], it[1] ] } \
        | combine(second) \
        | map{ [ it[0], [ "input1" : it[1], "input2" : it[3] ], it[2] ] } \
        | view { ["DEBUG2", it[0], it[1] ] } \
        | step2 \
        | view { ["DEBUG3", it[0], it[1] ] } \
        | toList() \
        | map { [ it[0][0], [ "input": it.collect{ it[1].values() }.flatten() ], it[0][2] ] } \
        | view { ["DEBUG4", it[0], it[1] ] } \
        | step3 \
        | view { ["DEBUG5", it[0], it[1] ] }
}
