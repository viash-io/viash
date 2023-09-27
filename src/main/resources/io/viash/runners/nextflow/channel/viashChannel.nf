
def viashChannel(params, config) {
  if (!viashChannelDeprecationWarningPrinted) {
    viashChannelDeprecationWarningPrinted = true
    System.err.println("Warning: viashChannel has deprecated in Viash 0.7.0. " +
                      "Please use a combination of channelFromParams and preprocessInputs.")
  }
  paramsToChannel(params, config)
    | map{tup -> [tup.id, tup]}
}
