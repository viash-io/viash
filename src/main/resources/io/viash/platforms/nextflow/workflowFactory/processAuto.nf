// TODO: unit test processAuto
def processAuto(Map auto) {
  // remove null values
  auto = auto.findAll{k, v -> v != null}

  expectedKeys = ["simplifyInput", "simplifyOutput", "transcript", "publish"]

  // check whether expected keys are all booleans (for now)
  for (key in expectedKeys) {
    assert auto.containsKey(key)
    assert auto[key] instanceof Boolean
  }

  return auto.subMap(expectedKeys)
}
