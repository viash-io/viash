
// Recurse upwards until we find a '.build.yaml' file
def _findBuildYamlFile(pathPossiblySymlink) {
  def path = pathPossiblySymlink.toRealPath()
  def child = path.resolve(".build.yaml")
  if (java.nio.file.Files.isDirectory(path) && java.nio.file.Files.exists(child)) {
    return child
  } else {
    def parent = path.getParent()
    if (parent == null) {
      return null
    } else {
      return _findBuildYamlFile(parent)
    }
  }
}

// get the root of the target folder
def getRootDir() {
  def dir = _findBuildYamlFile(moduleDir.normalize())
  assert dir != null: "Could not find .build.yaml in the folder structure"
  dir.getParent()
}
