lines3 = meta.resources_dir.resolve("resources/lines3.txt")
lines5 = meta.resources_dir.resolve("resources/lines5.txt")

workflow base {
  take: input_ch
  main:

  Channel.fromList([
    ["one", [input: [lines3]]],
    ["two", [input: [lines3, lines5]]],
    ["three", [input: [lines5]]]
  ])
    | step1.run(
      filter: { id, data -> id != "three" },
      runIf: { id, data -> data.input.size() == 2 || id == "three" }
    )
    | view{"output: $it"}
    | toSortedList{ a, b -> a[0] <=> b[0] }
    | view { tup_list ->
      assert tup_list.size() == 2 : "Output channel should contain 2 events. Expected: tup_list.size() == 2. Actual: tup_list.size() == ${tup_list.size()}"
      
      // check tuples
      def tup0 = tup_list[0]
      def tup1 = tup_list[1]
      assert tup0.size() == 2 : "Outputs should contain two elements; [id, output]. Expected: tup0.size() == 2. Actual: tup0.size() == ${tup0.size()}"
      assert tup1.size() == 2 : "Outputs should contain two elements; [id, output]. Expected: tup1.size() == 2. Actual: tup1.size() == ${tup1.size()}"

      // check id
      assert tup0[0] == "one" : "Id should be 'one'. Expected: tup0[0] == 'one'. Actual: tup0[0] == '${tup0[0]}'."
      assert tup1[0] == "two" : "Id should be 'two'. Expected: tup1[0] == 'two'. Actual: tup1[0] == '${tup1[0]}'."

      // check tup0 data
      assert tup0[1] instanceof Map : "Data should be a Map. Expected tup0[1] instanceof Map. Actual: tup0[1] instanceof ${tup0[1].getClass()}"
      assert tup0[1].containsKey("input") : "Data should contain key input. Actual: tup0[1].containsKey('input') == ${tup0[1].containsKey('input')}"
      assert tup0[1].input instanceof List : "Data.input should be a List. Expected tup0[1].input instanceof List. Actual: tup0[1].input instanceof ${tup0[1].input.getClass()}"
      assert tup0[1].input.size() == 1 : "Data.input should contain 1 file. Expected tup0[1].input.size() == 1. Actual: tup0[1].input.size() == ${tup0[1].input.size()}"
      assert tup0[1].input[0].name == "lines3.txt" : "Data.input should contain lines3.txt. Expected tup0[1].input[0].name == 'lines3.txt'. Actual: tup0[1].input[0].name == '${tup0[1].input[0].name}'"

      // check tup1 data
      assert tup1[1] instanceof Map : "Data should be a Map. Expected tup1[1] instanceof Map. Actual: tup1[1] instanceof ${tup1[1].getClass()}"
      assert tup1[1].containsKey("output") : "Data should contain key output. Actual: tup1[1].containsKey('output') == ${tup1[1].containsKey('output')}"
      assert tup1[1].output instanceof Path : "Data.output should be a Path. Expected tup1[1].output instanceof Path. Actual: tup1[1].output instanceof ${tup1[1].output.getClass()}"
      assert tup1[1].output.name == "two.step1.output.txt" : "Data.output should be lines3.txt. Expected tup1[1].output.name == 'lines3.txt'. Actual: tup1[1].output.name == '${tup1[1].output.name}'"

      ""
    }
  emit:
  input_ch
}
