
workflow base {
  take: input_ch
  main:

  // generate list from 0 to 1
  ch = Channel.fromList(0..1)
    | map { num ->
      // create temporary file
      file = tempFile()
      file.write("num: $num")

      ["num$num", [ num: num, file: file ]]
    }
    | concat.run(
      fromState: ["input": "input"],
      toState: {id, output, state -> 
        def newState = [
          "step1_output": output.output, 
          "num": state.num,
          "file": state.file
        ]
        return newState
      }
    )
    

  emit:
  input_ch
}
