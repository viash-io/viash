import io.viash.helpers.Resources

object CSharp extends Language {
  val id: String = "csharp"
  val name: String = "C#"
  val extensions: Seq[String] = Seq(".csx", ".cs")
  val commentStr: String = "//"
  val executor: Seq[String] = Seq("dotnet", "script")
  val viashParseYamlCode: String = Resources.read("languages/csharp/ViashParseYaml.csx")
}
