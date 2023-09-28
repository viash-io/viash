void writeJson(data, file) {
  assert data: "writeJson: data should not be null"
  assert file: "writeJson: file should not be null"
  file.write(toJsonBlob(data))
}
