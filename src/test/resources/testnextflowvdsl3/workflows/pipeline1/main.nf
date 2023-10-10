nextflow.enable.dsl=2

targetDir = "${params.rootDir}/target/nextflow"

// ["input": List[File]] -> File
include { step1 } from "$targetDir/step1/main.nf"

// ["input1": File, "input2": File, "optional": Option[File]] -> ["output1": File] and ["output2": File]
include { step2 } from "$targetDir/step2/main.nf"

// ["input": List[File]] -> File
include { step3 } from "$targetDir/step3/main.nf"

lines3 = file("${params.rootDir}/resources/lines3.txt")
lines5 = file("${params.rootDir}/resources/lines5.txt")

def channelValue = Channel.value([ 
  "foo", // id
  ["input": [lines3, lines5]],
  lines3 // pass-through
])

workflow base {
  channelValue
  | view{ "DEBUG1: $it" }
  // : Channel[(String, Map[String, List[File]], File)]
  //     * it[0]: a string identifier
  //     * it[1]: a map with a list of files
  //     * it[2]: a file

  | step1.run(
    auto: [simplifyOutput: true]
  )

  | view{ "DEBUG2: $it" }
  // : Channel[(String, File, File)]

  | map{ [ it[0], [ "input1" : it[1], "input2" : it[2] ] ] }
  | view{ "DEBUG3: $it" }
  // : Channel[(String, Map[String, File], File)]
  //     * it[1]: a map with two files (named input1 and input2)

  | step2.run(
    auto: [simplifyOutput: true]
  )

  | view{ "DEBUG4: $it" }
  // : Channel[(String, Map[String, File], File)]
  //     * it[1]: a map with two files (named input1 and input2)

  | map{ [ it[0], [ "input": [ it[1].output1 , it[1].output2 ] ] ] }
  | view{ "DEBUG5: $it" }
  // : Channel[(String, Map[String, List[File]])]
  //     * it[1]: a map with a list of files

  | step3.run(
    // test directives
    directives: [
      publishDir: "output/foo"
    ],
    auto: [simplifyOutput: true],
    // test debug
    debug: true
  )
  | view{ "DEBUG6: $it" }
  // : Channel[(String, File)]
}

workflow test_map_mapdata_mapid_arguments {
  channelValue
  | view{ "DEBUG1: $it" }
  | step1.run(
    auto: [simplifyOutput: true]
  )
  | view{ "DEBUG2: $it" }
  | step2.run(
    // test map
    map: { [ it[0], [ "input1" : it[1], "input2" : it[2] ] ] },
    auto: [simplifyOutput: true]
  )
  | view { "DEBUG3: $it" }
  | step3.run(
    // test id
    mapId: {it + "_bar"},
    // test mapdata
    mapData: { [ "input": [ it.output1 , it.output2 ] ] },
    auto: [simplifyOutput: true]
  )
  /* TESTING */
  | view{ "DEBUG4: $it"}
  | toList()
  | view { output_list ->
    assert output_list.size() == 1 : "output channel should contain 1 event"

    def output = output_list[0]
    assert output.size() == 2 : "outputs should contain two elements; [id, output]"
    def id = output[0]

    // check id
    assert id == "foo_bar" : "id should be foo_bar"

    // check final output file
    def output_str = output[1].readLines().join("\n")
    assert output_str.matches('^11 .*$') : 'output should match ^11 .*$'

    // return something to print
    "DEBUG5: $output"
  }
}

