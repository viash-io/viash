import io.viash.helpers.Resources

object Scala extends Language {
  val id: String = "scala"
  val name: String = "Scala"
  val extensions: Seq[String] = Seq(".scala")
  val commentStr: String = "//"
  val executor: Seq[String] = Seq("scala", "-nc")
  val viashParseYamlCode: String = Resources.read("languages/scala/ViashParseYaml.scala")
}
