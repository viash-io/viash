import io.viash.helpers.Resources

object Bash extends Language {
  val id: String = "bash"
  val name: String = "Bash"
  val extensions: Seq[String] = Seq(".sh")
  val commentStr: String = "#"
  val executor: Seq[String] = Seq("bash")
  val viashParseYamlCode: String = Resources.read("languages/bash/ViashParseYaml.sh")
}
