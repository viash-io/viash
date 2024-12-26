workflow base {
  take: input_ch
  main:

  // generate list from 0 to 1000
  ch = Channel.fromList(0..1000)
    | map { num ->
      // create temporary file
      def file = tempFile()
      file.write("num: $num")

      ["num$num", [ num: num, file: file ]]
    }

    // test fromstate and tostate with list[string]
    | step1.run(
      fromState: ["input": "file"],
      toState: ["step1_output": "output"]
    )
    
    | view{ id, state ->
      def num = state.num

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
    
    | step1.run(
      key: "step1bis",
      // TODO: test filter, runIf
      map: { id, state -> 
        def new_state = state + [
          "oid": id,
          "extra_key": "foo"
        ]
        [id, new_state]
      },
      fromState: { id, state ->
        ["input": state.file]
      },
      toState: { id, output, state ->
        def original_id = state.original_id
        def new_state = state.findAll{k, v -> k != "original_id"}
        
        ["${id}_modified", state + 
          ["step1bis_output": output.output] + 
          ["another_key": "bar"] + 
          ["oid": original_id]
        ]
      }
    )

    | view { id, state ->
      def num = state.num

      // check id
      assert id == "num${num}_modified": "id should be 'num${num}_modified'. Found: '$id'"

      // check orig id
      assert state.original_id == "num$num": "original_id should be 'num$num'. Found: '${state.original_id}'"

      // check file text
      def file_text = state.file.toFile().readLines()[0]
      assert file_text == "num: $num": "file text should be 'num: $num'. Found: '$file_text'"

      // check step1_output text
      def step1_output_text = state.step1_output.toFile().readLines()[0]
      assert step1_output_text == "num: $num": "step1_output text should be 'num: $num'. Found: '$step1_output_text'"

      // check step1bis_output text
      def step1bis_output_text = state.step1bis_output.toFile().readLines()[0]
      assert step1bis_output_text == "num: $num": "step1bis_output text should be 'num: $num'. Found: '$step1bis_output_text'"

      // check extra_key
      assert state.extra_key == "foo": "extra_key should be 'foo'. Found: '${state.extra_key}'"

      // check another_key
      assert state.another_key == "bar": "another_key should be 'bar'. Found: '${state.another_key}'"

      if (num == 0) {
        "after step1bis: id: $id, state: $state"
      } else {
        null
      }
    }

  emit:
  input_ch
}
