workflow run_wf {
  take: input
  main:
  input
    | view {"DEBUG: $it"}

  output_ch = Channel.empty()
  emit: output_ch
}