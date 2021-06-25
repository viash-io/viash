nextflow.enable.dsl=2

targetDir = "${params.rootDir}/target/nextflow"

// ["input": List[File]] -> File
include  { step1 }    from "$targetDir/step1/main.nf"       params(params)

// ["input1": File, "input2": File, "optional": Option[File]] -> ["output1": File] and ["output2": File]
include  { step2 }    from "$targetDir/step2/main.nf"       params(params)

// ["input": List[File]] -> File
include  { step3 }    from "$targetDir/step3/main.nf"       params(params)

workflow {
    second = Channel.fromPath("${params.rootDir}/resources/lines3.txt")
    
    first = Channel.fromPath("${params.rootDir}/resources/lines*.txt") \
        
        | view{ [ "DEBUG0", it ] } \
        // : Channel[File]

        | toList() \
        | view{ [ "DEBUG1", it ] } \
        // : Channel[List[File]]

        | map{ [ "foo", [ "input": it ], params ] } \
        | view{ [ "DEBUG2", it[0], it[1] ] } \
        // : Channel[(String, Map[String, List[File]], params)]
        //     * it[0]: a string identifier
        //     * it[1]: a map with a list of files
        //     * it[2]: the params object

        | step1 \
        | view{ [ "DEBUG2", it[0], it[1] ] } \
        // : Channel[(String, File, params)]

        | combine(second) \
        | view{ [ "DEBUG3", it[0], it[1], it[3]] } \
        // : Channel[(String, File, params, File)]

        | map{ [ it[0], [ "input1" : it[1], "input2" : it[3] ], it[2] ] } \
        | view{ [ "DEBUG4", it[0], it[1] ] } \
        // : Channel[(String, Map[String, File], params)]
        //     * it[1]: a map with two files (named input1 and input2)

        | step2 \
        | view { ["DEBUG5", it[0], it[1] ] } \
        // : Channel[(String, File, params)]
        //   One channel event in step2 generates two channel events, 
        //   one where it[1] is a map with name "output1" and 
        //   one where it[1] is a map with name "output2"

        | toList() \
        | map { out -> [ out[0][0], [ "input": out.collect{ it[1].values() }.flatten() ], out[0][2] ] } \
        | view { ["DEBUG6", it[0], it[1] ] } \
        // : Channel[(String, Map[String, List[File]], params)]
        //     * it[1]: a map with a list of files

        | step3 \
        | view { ["DEBUG7", it[0], it[1] ] }
        // : Channel[(String, File, params)]
}

// output of step2
// [
//   [ "id", ["output1": ...], params ],
//   [ "id", ["output2": ...], params ]
// ]

// After the NXF interface is refactored (see https://github.com/viash-io/viash/issues/60 ), 
// the output of step2 will probably be:
// [ "id", ["output1": ..., "output2": ...], params ]

// or even:
// [ "id", ["output1": ..., "output2": ...] ]
// 
// (if the params object gets removed and parameter overrides can be stored inside the second element)