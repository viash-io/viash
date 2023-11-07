workflow base {
  take: input_ch
  main:

  // generate list from 0 to 1000
  ch = Channel.fromList(0..1000)
    | map { num ->
      // create temporary file
      file = tempFile()
      file.write("num: $num")

      ["num$num", [ file: file ], ["num": num]]
    }
    | sub_workflow.run(
      toState: {id, output, state -> 
        def newState = [
          "step1_output": output.output, 
          "num": state.num,
          "file": state.file
        ]
        return newState
      }
    )
    | view{ id, state, extra ->
      def num = extra.num

      // check id
      assert id == "num$num": "id should be 'num$num'. Found: '$id'"

      // check file text
      def file_text = state.file.toFile().readLines()[0]
      assert file_text == "num: $num": "file text should be 'num: $num'. Found: '$file_text'"

      // check step1_output text
      def step1_output_text = state.step1_output.toFile().readLines()[0]
      assert step1_output_text == "num: $num": "step1_output text should be 'num: $num'. Found: '$step1_output_text'"

      if (num == 0) {
        "after step1: id: $id, state: $state"
      } else {
        null
      }
    }
    

  emit:
  input_ch
}
