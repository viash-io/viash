lines3 = meta.resources_dir.resolve("resources/lines3.txt")
lines5 = meta.resources_dir.resolve("resources/lines5.txt")

workflow base {
  take: input_ch
  main:

  Channel.fromList([
    ["one", [input: [lines3, lines5], other: lines3]],
    ["two", [input: [lines3, lines5], other: lines5]]
  ])
    | view{ "DEBUG1: $it" }

    // test fromstate and tostate with list[string]
    | step1.run(
      fromState: ["input"],
      toState: ["output"]
    )
    | view{ "DEBUG2: $it" }

    // test fromstate and tostate with map[string, string]
    | step2.run(
      fromState: ["input1": "output", "input2": "other"],
      toState: ["step2_output1": "output1", "step2_output2": "output2"]
    )
    | view{ "DEBUG3: $it" }

    // test fromstate and tostate with closure
    | step3.run(
      fromState: { id, state ->
        [ "input": [state.step2_output1, state.step2_output2] ]
      },
      toState: { id, output, state ->
        state + [ "step3_output": output.output ]
      }
    )
    /* TESTING */
    | toSortedList{ a, b -> a[0] <=> b[0] }
    | view { tups ->
      assert tups.size() == 2 : "Output channel should contain 2 event. Expected: output_list.size() == 2. Actual: output_list.size() == ${output_list.size()}"

      // check tuples
      def tup0 = tups[0]
      def tup1 = tups[1]
      assert tup0.size() == 2 : "Outputs should contain two elements; [id, output]. Expected: tup0.size() == 2. Actual: tup0.size() == ${tup0.size()}"
      assert tup1.size() == 2 : "Outputs should contain two elements; [id, output]. Expected: tup1.size() == 2. Actual: tup1.size() == ${tup1.size()}"

      // check id
      assert tup0[0] == "one" : "Id should be 'one'. Expected: tup0[0] == 'one'. Actual: tup0[0] == '${tup0[0]}'."
      assert tup1[0] == "two" : "Id should be 'two'. Expected: tup1[0] == 'two'. Actual: tup1[0] == '${tup1[0]}'."

      // check data
      def expectedKeys = ["input", "other", "output", "step2_output1", "step2_output2", "step3_output"]

      // check tup0 data
      assert tup0[1] instanceof Map : "Output should be a Map. Expected: tup0[1] instanceof Map. Actual: tup0[1] instanceof ${tup0[1].getClass()}."
      assert tup0[1].keySet().containsAll(expectedKeys) : "Output should contain keys $expectedKeys. Actual: tup0[1].keySet() == ${tup0[1].keySet()}."
      def unexpectedKeys = tup0[1].keySet() - expectedKeys
      assert unexpectedKeys.size() == 0 : "Output should not contain keys $unexpectedKeys. Actual: tup0[1].keySet() == ${tup0[1].keySet()}."
      for (key in expectedKeys) {
        assert tup0[1][key] != null : "Output should contain key $key. Actual: tup0[1].keySet() == ${tup0[1].keySet()}."
        if (key == "input") {
          assert tup0[1][key] instanceof List : "data.input should be a List. Expected: tup0[1].input instanceof List. Actual: tup0[1].input instanceof ${tup0[1].input.getClass()}."
        } else {
          assert tup0[1][key] instanceof Path : "data.$key should be a Path. Expected: tup0[1].$key instanceof Path. Actual: tup0[1].$key instanceof ${tup0[1][key].getClass()}."
        }
      }

      // check tup1 data
      assert tup1[1] instanceof Map : "Output should be a Map. Expected: tup1[1] instanceof Map. Actual: tup1[1] instanceof ${tup1[1].getClass()}."
      assert tup1[1].keySet().containsAll(expectedKeys) : "Output should contain keys $expectedKeys. Actual: tup1[1].keySet() == ${tup1[1].keySet()}."
      unexpectedKeys = tup1[1].keySet() - expectedKeys
      assert unexpectedKeys.size() == 0 : "Output should not contain keys $unexpectedKeys. Actual: tup1[1].keySet() == ${tup1[1].keySet()}."
      for (key in expectedKeys) {
        assert tup1[1][key] != null : "Output should contain key $key. Actual: tup1[1].keySet() == ${tup1[1].keySet()}."
        if (key == "input") {
          assert tup1[1][key] instanceof List : "data.input should be a List. Expected: tup1[1].input instanceof List. Actual: tup1[1].input instanceof ${tup1[1].input.getClass()}."
        } else {
          assert tup1[1][key] instanceof Path : "data.$key should be a Path. Expected: tup1[1].$key instanceof Path. Actual: tup1[1].$key instanceof ${tup1[1][key].getClass()}."
        }
      }
      
      // return something to print
      "DEBUG4: $tups"
    }
    
  emit:
  input_ch
}
