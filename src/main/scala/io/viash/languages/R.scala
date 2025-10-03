import io.viash.helpers.Resources

object R extends Language {
  val id: String = "r"
  val name: String = "R"
  val extensions: Seq[String] = Seq(".R", ".r")
  val commentStr: String = "#"
  val executor: Seq[String] = Seq("Rscript")
  val viashParseYamlCode: String = Resources.read("languages/r/ViashParseYaml.R")
}
