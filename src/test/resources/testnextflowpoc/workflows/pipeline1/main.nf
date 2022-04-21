nextflow.enable.dsl=2

targetDir = "${params.rootDir}/target/nextflowpoc"

// ["input": List[File]] -> File
include  { step1 }    from "$targetDir/step1/main.nf"

// ["input1": File, "input2": File, "optional": Option[File]] -> ["output1": File] and ["output2": File]
include  { step2 }    from "$targetDir/step2/main.nf"

// ["input": List[File]] -> File
include  { step3 }    from "$targetDir/step3/main.nf"

workflow {
    Channel.value([ 
          "foo", // id
          [  // data
            "input": file("${params.rootDir}/resources/lines*.txt")
          ],
          file("${params.rootDir}/resources/lines3.txt") // pass-through
        ])
        | view{ "DEBUG1: $it" }
        // : Channel[(String, Map[String, List[File]], params)]
        //     * it[0]: a string identifier
        //     * it[1]: a map with a list of files
        //     * it[2]: a file

        | step1
        | view{ "DEBUG2: $it" }
        // : Channel[(String, File, File)]

        | step2.run(
            mapData: { [ "input1" : it[1], "input2" : it[2] ] },
            // : Channel[(String, Map[String, File], File)]
            //     * it[1]: a map with two files (named input1 and input2)
        )
        | view { "DEBUG3: $it" }
        // : Channel[(String, Map[String, File], File)]
        //     * it[1]: a map with two files (named input1 and input2)

        // strip off passthrough
        | map{ [ it[0], it[1] ] }
        // : Channel[(String, Map[String, File])]

        | step3.run(
            mapData: { [ "input": [ it[1].output1 , it[1].output2 ] ] },
            // : Channel[(String, Map[String, List[File]])]
            //     * it[1]: a map with a list of files
            directives: [
                publishDir: "output/foo"
            ],
        )
        | view { "DEBUG4: $it" }
        // : Channel[(String, File)]
}
