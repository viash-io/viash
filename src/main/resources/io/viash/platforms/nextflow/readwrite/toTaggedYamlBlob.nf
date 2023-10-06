// Custom representer to modify how certain objects are represented in YAML
class CustomRepresenter extends org.yaml.snakeyaml.representer.Representer {
  Path relativizer

  class RepresentPath implements org.yaml.snakeyaml.representer.Represent {
    public String getFileName(Object obj) {
      if (obj instanceof File) {
        obj = ((File) obj).toPath();
      }
      if (obj !instanceof Path) {
        throw new IllegalArgumentException("Object: " + obj + " is not a Path or File");
      }
      def path = (Path) obj;

      if (relativizer != null) {
        return relativizer.relativize(path).toString()
      } else {
        return path.toString()
      }
    }

    public org.yaml.snakeyaml.nodes.Node representData(Object data) {
      String filename = getFileName(data);
      def tag = new org.yaml.snakeyaml.nodes.Tag("!file");
      return representScalar(tag, filename);
    }
  }
  CustomRepresenter(org.yaml.snakeyaml.DumperOptions options, Path relativizer) {
    super(options)
    this.relativizer = relativizer
    this.representers.put(sun.nio.fs.UnixPath, new RepresentPath())
    this.representers.put(Path, new RepresentPath())
    this.representers.put(File, new RepresentPath())
  }
}

String toTaggedYamlBlob(data) {
  return toRelativeTaggedYamlBlob(data, null)
}
String toRelativeTaggedYamlBlob(data, Path relativizer) {
  def options = new org.yaml.snakeyaml.DumperOptions()
  options.setDefaultFlowStyle(org.yaml.snakeyaml.DumperOptions.FlowStyle.BLOCK)
  def representer = new CustomRepresenter(options, relativizer)
  def yaml = new org.yaml.snakeyaml.Yaml(representer, options)
  return yaml.dump(data)
}
