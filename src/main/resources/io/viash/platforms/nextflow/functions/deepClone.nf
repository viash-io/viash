/**
  * Performs a deep clone of the given object.
  * @param x an object
  */
def deepClone(x) {
  iterateMap(x, {it instanceof Cloneable ? it.clone() : it})
}