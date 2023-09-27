void writeYaml(data, file) {
  assert data: "writeYaml: data should not be null"
  assert file: "writeYaml: file should not be null"
  file.write(toYamlBlob(data))
}
