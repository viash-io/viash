workflow base {
  take: input
  main:
  output = input
    | step1.run(
      fromState: ["input": "input1"],
      toState: ["step1_output": "output"],
      auto: [simplifyOutput: false]
    )
    | step2.run(
      fromState: ["input1": "step1_output", "input2": "input2"],
      toState: ["step2_output1": "output1", "step2_output2": "output2"],
      auto: [simplifyOutput: false]
    )
    | step3.run(
      fromState: { id, state ->
        [
          input: [ state.step2_output1, state.step2_output2 ],
          output: state.output // set filename of output
        ]
      },
      toState: { id, output, state ->
        output
      },
      auto: [simplifyOutput: false]
    )
  emit: output
}
