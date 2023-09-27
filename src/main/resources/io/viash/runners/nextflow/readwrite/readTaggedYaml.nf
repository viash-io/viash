// Custom constructor to modify how certain objects are parsed from YAML
class CustomConstructor extends org.yaml.snakeyaml.constructor.Constructor {
  Path root

  class ConstructFile extends org.yaml.snakeyaml.constructor.AbstractConstruct {
    public Object construct(org.yaml.snakeyaml.nodes.Node node) {
      String filename = (String) constructScalar(node);
      if (root != null) {
        return root.resolve(filename);
      }
      return java.nio.file.Paths.get(filename);
    }
  }

  CustomConstructor(org.yaml.snakeyaml.LoaderOptions options, Path root) {
    super(options)
    this.root = root
    // Handling !file tag and parse it back to a File type
    this.yamlConstructors.put(new org.yaml.snakeyaml.nodes.Tag("!file"), new ConstructFile())
  }
}

def readTaggedYaml(Path path) {
  def options = new org.yaml.snakeyaml.LoaderOptions()
  def constructor = new CustomConstructor(options, path.getParent())
  def yaml = new org.yaml.snakeyaml.Yaml(constructor)
  return yaml.load(path.text)
}
