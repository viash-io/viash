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
      fromState: [
        "file": "file",
        "thisargumentdoesnotexist": "file", // this should raise
      ],
      toState: {id, output, state -> 
        def newState = [
          "step1_output": output.output, 
          "num": state.num,
          "file": state.file,
          "thisargumentdoesnotexist": "foo" 
        ]
        return newState
      }
    )
    

  emit:
  input_ch
}
