/**
  * A view for printing the event of each channel as a YAML blob.
  * This is useful for debugging.
  */
def niceView() {
  workflow niceViewWf {
    take: input
    main:
      output = input
        | view{toYamlBlob(it)}
    emit: output
  }
  return niceViewWf
}
