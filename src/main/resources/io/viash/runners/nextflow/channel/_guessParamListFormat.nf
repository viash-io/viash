
def _guessParamListFormat(params) {
  if (!params.containsKey("param_list") || params.param_list == null) {
    "none"
  } else {
    def param_list = params.param_list

    if (param_list !instanceof String) {
      "asis"
    } else if (param_list.endsWith(".csv")) {
      "csv"
    } else if (param_list.endsWith(".json") || param_list.endsWith(".jsn")) {
      "json"
    } else if (param_list.endsWith(".yaml") || param_list.endsWith(".yml")) {
      "yaml"
    } else {
      "yaml_blob"
    }
  }
}
