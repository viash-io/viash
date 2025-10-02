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
      ],
      toState: [
        "step1_output": "output",
        "file": "file",
        "newkey": "thisargumentdoesnotexist" // This should raise
      ]
    )


  emit:
  input_ch
}
