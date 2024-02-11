lines3 = meta.resources_dir.resolve("resources/lines3.txt")
lines5 = meta.resources_dir.resolve("resources/lines5.txt")

workflow base {
  take: input_ch
  main:

  assert step1.getName() == "step1"
  assert step1_alias.getName() == "step1_alias"

  
  Channel.fromList([
    ["one", [input: lines3]]
  ])
    | step1.run(
      fromState: [input: "input"],
      toState: [output1: "output"]
    )
    | step1_alias.run(
      fromState: [input: "output1"],
      toState: [output2: "output"]
    )
    | view{"Debug: $it"}
    | view { id, data ->
      assert id == "one" : "id should be one"
      assert data.input.getFileName().toString() == "lines3.txt"
      assert data.output1.getFileName().toString() == "one.step1.output.txt"
      assert data.output2.getFileName().toString() == "one.step1_alias.output.txt"

      ""
    }

  emit:
  input_ch
}
