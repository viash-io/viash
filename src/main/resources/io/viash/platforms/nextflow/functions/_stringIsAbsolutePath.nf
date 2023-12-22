/**
  * Check whether a path as a string is absolute.
  *
  * In the past, we tried using `file(., relative: true).isAbsolute()`,
  * but the 'relative' option was added in 22.10.0.
  *
  * @param path The path to check, as a String.
  *
  * @return Whether the path is absolute, as a boolean.
  */
def _stringIsAbsolutePath(path) {
  _resolve_URL_PROTOCOL = ~/^([a-zA-Z][a-zA-Z0-9]*:)?\\/.+/

  assert path instanceof String
  return _resolve_URL_PROTOCOL.matcher(path).matches()
}