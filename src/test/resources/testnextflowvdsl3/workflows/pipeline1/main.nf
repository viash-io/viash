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


workflow test_fromstate_tostate_arguments {
  Channel.fromList([ 
    [
      // id
      "foo",
      // data
      [
        "input": file("${params.rootDir}/resources/lines*.txt"),
        "lines3": file("${params.rootDir}/resources/lines3.txt")
      ]
    ] 
  ])
  | view{ "DEBUG1: $it" }

  // test fromstate and tostate with list[string]
  | step1.run(
    fromState: ["input"],
    toState: ["output"],
    auto: [simplifyOutput: false]
  )
  | view{ "DEBUG2: $it" }

  // test fromstate and tostate with map[string, string]
  | step2.run(
    fromState: ["input1": "output", "input2": "lines3"],
    toState: ["step2_output1": "output1", "step2_output2": "output2"],
    auto: [simplifyOutput: false]
  )
  | view{ "DEBUG3: $it" }

  // test fromstate and tostate with closure
  | step3.run(
    fromState: { id, state ->
      [ "input": [state.step2_output1, state.step2_output2] ]
    },
    toState: { id, output, state ->
      state + [ "step3_output": output.output ]
    },
    auto: [simplifyOutput: false]
  )
  /* TESTING */
  | toList()
  | view { output_list ->
    assert output_list.size() == 1 : "output channel should contain 1 event"

    def output = output_list[0]
    assert output.size() == 2 : "outputs should contain two elements; [id, state]"
    def id = output[0]
    def state = output[1]

    // check id
    assert id == "foo" : "id should be foo"

    // check state
    for (key in ["input", "lines3", "output", "step2_output1", "step2_output2", "step3_output"]) {
      assert state.containsKey(key) : "state should contain key $key"
      def value = state[key]
      if (key == "input") {
        assert value instanceof List : "state[input] should be a List"
      } else {
        assert value instanceof Path : "state[$key] should be a Path"
      }
    }

    // check final output file
    def output_str = state["step3_output"].readLines().join("\n")
    assert output_str.matches('^11 .*$') : 'output should match ^11 .*$'

    // return something to print
    "DEBUG4: $output"
  }
}


workflow test_filter_runif_arguments {
  Channel.fromList([
    ["one", [input: [lines3]]],
    ["two", [input: [lines3, lines5]]],
    ["three", [input: [lines5]]]
  ])
  | step1.run(
    filter: { id, data -> id != "three" },
    runIf: { id, data -> data.input.size() == 2 }
  )
  | toSortedList( { a, b -> a[0] <=> b[0] } )
  | view { tup_list ->
    assert tup_list.size() == 2 : "output channel should contain 1 event"

    def tup0 = tup_list[0]
    assert tup0.size() == 2 : "outputs should contain two elements; [id, output]"

    // check id
    assert tup0[0] == "one" : "id should be one"
    assert tup1[0] == "two" : "id should be two"

    // check data
    assert tup[0].containsKey("input") : "data should contain key input"
    assert tup[0].input.size() == 1 : "data should contain 1 file"
    assert tup[0].input[0].name == "lines3.txt" : "input should contain lines3.txt"

    assert tup[1].containsKey("output") : "data should contain key output"
    assert tup[1].output == 1 : "data should contain 1 file"
    assert tup[1].output.name == "lines3.txt" : "input should contain lines3.txt"

    ""
  }
}