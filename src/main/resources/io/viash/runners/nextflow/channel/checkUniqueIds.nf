def checkUniqueIds(Map args) {
  def stopOnError = args.stopOnError == null ? args.stopOnError : true

  def idChecker = new IDChecker()

  return filter { tup ->
    if (!idChecker.observe(tup[0])) {
      if (stopOnError) {
        error "Duplicate id: ${tup[0]}"
      } else {
        log.warn "Duplicate id: ${tup[0]}, removing duplicate entry"
        return false
      }
    }
    return true
  }
}