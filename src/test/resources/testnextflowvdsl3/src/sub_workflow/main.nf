workflow base {
  take: input_ch
  main:
  
    output_ch = input_ch
    | step1.run(
      fromState: ["input": "file"],
      toState: { id, output, state -> 
        def newState = ["output":  output.output]
        return newState
      }
    )
    | map {id, state ->
      def newState = state + [required_int: 1, multiple_strings: ["a", "b"]]
      [id, newState]
    }
    
  emit:
  output_ch
}
