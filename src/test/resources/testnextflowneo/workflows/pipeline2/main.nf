nextflow.enable.dsl=2

targetDir = "${params.rootDir}/target/nextflow"
targetNeoDir = "${params.rootDir}/target/nextflowneo"

// ["input": List[File]] -> File
include  { step1 }    from "$targetDir/step1/main.nf" params(params)

// ["input1": File, "input2": File, "optional": Option[File]] -> ["output1": File] and ["output2": File]
include  { step2 }    from "$targetDir/step2/main.nf" params(params)
include  { step2 as step2neo }    from "$targetNeoDir/step2/main.nf"

// ["input": List[File]] -> File
include  { step3 }    from "$targetDir/step3/main.nf" params(params)

def channelValue = Channel.value([ 
          "foo", // id
          [  // data
            "input": file("${params.rootDir}/resources/lines*.txt")
          ],
          params,
        //   file("${params.rootDir}/resources/lines3.txt") // pass-through
        ])

workflow legacy_base {
        second = Channel.fromPath("${params.rootDir}/resources/lines3.txt")

        channelValue
        | view { "DEBUG1: [${it[0]}, ${it[1]}]" }
        // : Channel[(String, Map[String, List[File]], params)]
        //     * it[0]: a string identifier
        //     * it[1]: a map with a list of files
        //     * it[2]: a file

        | step1
        | view { "DEBUG2: [${it[0]}, ${it[1]}]" }
        // : Channel[(String, File, File)]

        | combine(second)
        | map{ [ it[0], [ "input1" : it[1], "input2" : it[3] ], it[2] ] }
        | view { "DEBUG3: [${it[0]}, ${it[1]}]" }
        // : Channel[(String, Map[String, File], File)]
        //     * it[1]: a map with two files (named input1 and input2)

        | step2neo
        | view { "DEBUG4: [${it[0]}, ${it[1]}]" }
        // : Channel[(String, Map[String, File], File)]
        //     * it[1]: a map with two files (named input1 and input2)

        | toList()
        | map { out -> [ out[0][0], [ "input": out.collect{ it[1].values() }.flatten() ], out[0][2] ] }
        | view { "DEBUG5: [${it[0]}, ${it[1]}]" }
        // : Channel[(String, Map[String, List[File]], params)]
        //     * it[1]: a map with a list of files

        | step3
        | view { "DEBUG6: [${it[0]}, ${it[1]}]" }
}

workflow legacy_and_neo {
        second = Channel.fromPath("${params.rootDir}/resources/lines3.txt")

        channelValue
        | view { "DEBUG1: [${it[0]}, ${it[1]}]" }
        | step1
        | view { "DEBUG2: [${it[0]}, ${it[1]}]" }
        | combine(second)
        | map{ [ it[0], [ "input1" : it[1], "input2" : it[3] ], it[2] ] }
        | view { "DEBUG3: [${it[0]}, ${it[1]}]" }
        | step2neo
        | view { "DEBUG4: [${it[0]}, ${it[1]}]" }
        | map{ [ it[0], [ "input": [ it[1].output1 , it[1].output2 ] ], it[2] ] }
        | view { "DEBUG5: [${it[0]}, ${it[1]}]" }
        | step3
        | view { "DEBUG6: [${it[0]}, ${it[1]}]" }
}
