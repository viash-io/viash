workflow base {
  take: input_ch
  main:

  // generate list from 0 to 1000
  ch = Channel.fromList(0..1000)
    | map { num ->
      // create temporary file
      def file = tempFile()
      file.write("num: $num")

      ["num$num", [ file: file ], ["num": num]]
    }
    | sub_workflow.run(
      toState: ["output", "required_int", "multiple_strings"]
    )
    | view{ id, state, extra ->
      def num = extra.num

      // check id
      assert id == "num$num": "id should be 'num$num'. Found: '$id'"

      // check file text
      def file_text = state.file.toFile().readLines()[0]
      assert file_text == "num: $num": "file text should be 'num: $num'. Found: '$file_text'"

      // check output text
      def output_text = state.output.toFile().readLines()[0]
      assert output_text == "num: $num": "output text should be 'num: $num'. Found: '$output_text'"

      // check required int
      assert state.required_int == 1: "Expected required_int to be 1, found ${state.required_int}"
      
      // check multiple output
      assert state.multiple_strings == ["a", "b"]: "Expected multiple_strings to be ['a', 'b'], found ${state.multiple_strings}"

      if (num == 0) {
        "after sub_workflow: id: $id, state: $state"
      } else {
        null
      }
    }
    

  emit:
  input_ch
}
