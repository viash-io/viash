import io.viash.helpers.Resources

object JavaScript extends Language {
  val id: String = "javascript"
  val name: String = "JavaScript"
  val extensions: Seq[String] = Seq(".js")
  val commentStr: String = "//"
  val executor: Seq[String] = Seq("node")
  val viashParseYamlCode: String = Resources.read("languages/javascript/ViashParseYaml.js")
}
