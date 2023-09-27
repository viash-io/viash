def paramsToChannel(params, config) {
  if (!viashChannelDeprecationWarningPrinted) {
    viashChannelDeprecationWarningPrinted = true
    System.err.println("Warning: paramsToChannel has deprecated in Viash 0.7.0. " +
                      "Please use a combination of channelFromParams and preprocessInputs.")
  }
  Channel.fromList(paramsToList(params, config))
}
