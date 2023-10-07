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
      runIf: { id, data -> data.input.size() == 2 }
    )
    | view{"output: $it"}
    | toList()
    | view { tup_list ->
      assert tup_list.size() == 2 : "output channel should contain 2 events"

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
  emit:
  Channel.empty()
}