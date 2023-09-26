class IDChecker {
  final def items = [] as Set

  @groovy.transform.WithWriteLock
  boolean isUnique(String item) {
    if (items.contains(item)) {
      return false
    } else {
      items << item
      return true
    }
  }
}

def checkUniqueIds(Map args) {
  def stopOnError = args.stopOnError == null ? args.stopOnError : true

  def idChecker = new IDChecker()

  return filter { tup ->
    if (!idChecker.isUnique(tup[0])) {
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