import io.viash.helpers.Resources

object Python extends Language {
  val id: String = "python"
  val name: String = "Python"
  val extensions: Seq[String] = Seq(".py")
  val commentStr: String = "#"
  val executor: Seq[String] = Seq("python", "-B")
  val viashParseYamlCode: String = Resources.read("languages/python/ViashParseYaml.py")
}
