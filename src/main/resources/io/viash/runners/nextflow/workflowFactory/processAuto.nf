// TODO: unit test processAuto
def processAuto(Map auto) {
  // remove null values
  auto = auto.findAll{k, v -> v != null}

  // check for unexpected keys
  def expectedKeys = ["simplifyInput", "simplifyOutput", "transcript", "publish"]
  def unexpectedKeys = auto.keySet() - expectedKeys
  assert unexpectedKeys.isEmpty(), "unexpected keys in auto: '${unexpectedKeys.join("', '")}'"

  // check auto.simplifyInput
  assert auto.simplifyInput instanceof Boolean, "auto.simplifyInput must be a boolean"

  // check auto.simplifyOutput
  assert auto.simplifyOutput instanceof Boolean, "auto.simplifyOutput must be a boolean"

  // check auto.transcript
  assert auto.transcript instanceof Boolean, "auto.transcript must be a boolean"

  // check auto.publish
  assert auto.publish instanceof Boolean || auto.publish == "state", "auto.publish must be a boolean or 'state'"

  return auto.subMap(expectedKeys)
}
