
// helper functions for reading params from file //
def _getChild(parent, child) {
  if (child.contains("://") || java.nio.file.Paths.get(child).isAbsolute()) {
    child
  } else {
    def parentAbsolute = java.nio.file.Paths.get(parent).toAbsolutePath().toString()
    parentAbsolute.replaceAll('/[^/]*$', "/") + child
  }
}
