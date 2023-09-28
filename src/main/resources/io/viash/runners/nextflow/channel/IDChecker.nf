class IDChecker {
  final def items = [] as Set

  @groovy.transform.WithWriteLock
  boolean observe(String item) {
    if (items.contains(item)) {
      return false
    } else {
      items << item
      return true
    }
  }

  @groovy.transform.WithReadLock
  boolean contains(String item) {
    return items.contains(item)
  }

  @groovy.transform.WithReadLock
  Set getItems() {
    return items.clone()
  }
}