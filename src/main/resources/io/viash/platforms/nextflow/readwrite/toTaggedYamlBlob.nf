// Custom representer to modify how certain objects are represented in YAML
class CustomRepresenter extends org.yaml.snakeyaml.representer.Representer {
  class RepresentFile implements org.yaml.snakeyaml.representer.Represent {
    public org.yaml.snakeyaml.nodes.Node representData(Object data) {
      File file = (File) data;
      def value = file.name;
      def tag = new org.yaml.snakeyaml.nodes.Tag("!file");
      return representScalar(tag, value);
    }
  }
  CustomRepresenter(org.yaml.snakeyaml.DumperOptions options) {
    super(options)
    this.representers.put(File, new RepresentFile())
  }
}

String toTaggedYamlBlob(Map data) {
  def options = new org.yaml.snakeyaml.DumperOptions()
  options.setDefaultFlowStyle(org.yaml.snakeyaml.DumperOptions.FlowStyle.BLOCK)
  def representer = new CustomRepresenter(options)
  def yaml = new org.yaml.snakeyaml.Yaml(representer, options)
  return yaml.dump(data)
}
