import io.viash.helpers.Resources

object Nextflow extends Language {
  val id: String = "nextflow"
  val name: String = "Nextflow"
  val extensions: Seq[String] = Seq(".nf")
  val commentStr: String = "//"
  val executor: Seq[String] = Seq("nextflow", "run", ".", "-main-script")
  // this is processed in a different way
  val viashParseYamlCode: String = ""
}
