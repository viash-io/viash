workflow base {
  take: input
  main:
  output = input
    | step1.run(
      fromState: ["input": "input1"],
      toState: ["step1_output": "output"],
      auto: [simplifyOutput: false]
    )
    | step2.run(
      fromState: ["input1": "step1_output", "input2": "input2"],
      toState: ["step2_output1": "output1", "step2_output2": "output2"],
      auto: [simplifyOutput: false]
    )
    | step3.run(
      fromState: { id, state ->
        [
          input: [ state.step2_output1, state.step2_output2 ],
          output: state.output // set filename of output
        ]
      },
      toState: { id, output, state ->
        output
      },
      auto: [simplifyOutput: false]
    )
  emit: output
}

workflow test_base {
  // todo: fix how `test_base` is able to access the test resources
  Channel.value([ 
    "foo",
    [
      "input1": file("${params.rootDir}/resources/lines*.txt"),
      "input2": file("${params.rootDir}/resources/lines3.txt")
    ]
  ])
    | view{ "DEBUG1: $it" }
    | wf
    /* TESTING */
    | view{ "DEBUG4: $it"}
    | toList()
    | view { output_list ->
      assert output_list.size() == 1 : "output channel should contain 1 event"

      def event = output_list[0]
      assert event.size() == 2 : "outputs should contain two elements; [id, state]"
      def id = event[0]

      // check id
      assert id == "foo" : "id should be foo"

      // check state
      def state = event[1]
      assert state instanceof Map : "state should be a map"
      assert "output" in state : "state should contain key 'output'"

      // check final output file
      def output = state.output
      assert output instanceof Path: "output should be a file"
      assert output.toFile().exists() : "output file should exist"

      def output_str = output.readLines().join("\n")
      assert output_str.matches('^11 .*$') : 'output should match ^11 .*$'

      // return something to print
      "DEBUG5: $event"
    }
}