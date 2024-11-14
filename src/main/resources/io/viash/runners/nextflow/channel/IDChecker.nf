class IDChecker {
  final def items = Collections.synchronizedSet(new HashSet())

  @groovy.transform.Synchronized
  boolean observe(String item) {
    if (items.contains(item)) {
      return false
    } else {
      items << item
      return true
    }
  }

  @groovy.transform.Synchronized
  boolean contains(String item) {
    return items.contains(item)
  }

  @groovy.transform.Synchronized 
  Set getItems() {
    return items.clone()
  }
}