// Custom representer to modify how certain objects are represented in YAML
class CustomRepresenter extends org.yaml.snakeyaml.representer.Representer {
  class RepresentPath implements org.yaml.snakeyaml.representer.Represent {
    public String getFileName(Object obj) {
      if (obj instanceof Path) {
        def file = (Path) obj;
        return file.getFileName();
      } else if (obj instanceof File) {
        def file = (File) obj;
        return file.getName();
      } else {
        throw new IllegalArgumentException("Object: " + obj + " is not a Path or File");
      }
    }

    public org.yaml.snakeyaml.nodes.Node representData(Object data) {
      String filename = getFileName(data);
      def tag = new org.yaml.snakeyaml.nodes.Tag("!file");
      return representScalar(tag, filename);
    }
  }
  CustomRepresenter(org.yaml.snakeyaml.DumperOptions options) {
    super(options)
    this.representers.put(sun.nio.fs.UnixPath, new RepresentPath())
    this.representers.put(Path, new RepresentPath())
    this.representers.put(File, new RepresentPath())
  }
}

String toTaggedYamlBlob(Map data) {
  def options = new org.yaml.snakeyaml.DumperOptions()
  options.setDefaultFlowStyle(org.yaml.snakeyaml.DumperOptions.FlowStyle.BLOCK)
  def representer = new CustomRepresenter(options)
  def yaml = new org.yaml.snakeyaml.Yaml(representer, options)
  return yaml.dump(data)
}
