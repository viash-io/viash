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
    
  emit:
  output_ch
}
