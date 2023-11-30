/**
  * Resolve a path relative to the current file.
  * 
  * @param str The path to resolve, as a String.
  * @param parentPath The path to resolve relative to, as a Path.
  *
  * @return The path that may have been resovled, as a Path.
  */

def _resolveSiblingIfNotAbsolute(str, parentPath) {
  if (str !instanceof String) {
    return str
  }
  strAsPath = file(str, hidden: true, relative: true)
  if (!strAsPath.isAbsolute()) {
    return parentPath.resolveSibling(str) 
  }
  return strAsPath
}