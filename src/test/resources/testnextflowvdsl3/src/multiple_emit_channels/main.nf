workflow base {
  take: input_ch
  main:

    step_1_ch = input_ch
    // test fromstate and tostate with list[string]
    | step1.run(
      fromState: ["input"],
      toState: { id, output, state -> ["step_1_output": output.output, "multiple_output": output.output] }
    )

    step_3_ch = input_ch
    // test fromstate and tostate with map[string, string]
    | step3.run(
      fromState: ["input"],
      toState: { id, output, state -> ["step_3_output": output.output, "multiple_output": output.output] }
    )
  
  emit:
    step_1_ch
    step_3_ch
}

workflow test_base {
  // todo: fix how `test_base` is able to access the test resources
  Channel.value([ 
    "foo",
    [
      "input": file("${params.rootDir}/resources/lines3.txt")
    ]
  ])
    | multiple_emit_channels
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
      assert "step_1_output" in state : "state should contain key 'step_1_output'"
      assert "step_3_output" in state : "state should contain key 'step_3_output'"
 
      def step_1_output = state.step_1_output
      assert step_1_output instanceof Path: "step_1_output should be a file"
      assert step_1_output.toFile().exists() : "step_1_output file should exist"

      def step_3_output = state.step_3_output
      assert step_3_output instanceof Path: "step_3_output should be a file"
      assert step_3_output.toFile().exists() : "step_3_output file should exist"

      assert "multiple_output" in state : "state should contain 'multiple_output'"
      def multiple_output = state.multiple_output
      assert multiple_output instanceof List: "multiple_output should be a list"
      assert multiple_output.size() ==  2
    }
}