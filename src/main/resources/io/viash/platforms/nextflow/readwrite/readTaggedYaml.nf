// Custom constructor to modify how certain objects are parsed from YAML
class CustomConstructor extends org.yaml.snakeyaml.constructor.Constructor {
  File root

  class ConstructFile extends org.yaml.snakeyaml.constructor.AbstractConstruct {
    public Object construct(org.yaml.snakeyaml.nodes.Node node) {
      String filename = (String) constructScalar(node);
      if (root != null) {
        return new File(root, filename);
      }
      return new File(filename);
    }
  }

  CustomConstructor(File root = null) {
    super()
    this.root = root
    // Handling !file tag and parse it back to a File type
    this.yamlConstructors.put(new org.yaml.snakeyaml.nodes.Tag("!file"), new ConstructFile())
  }
}

def readTaggedYaml(File file) {
  def constructor = new CustomConstructor(file.absoluteFile.parentFile)
  def yaml = new org.yaml.snakeyaml.Yaml(constructor)
  return yaml.load(file.text)
}
