/**
 * Represents a programming language.
 */
trait Language {
  // The unique identifier for the programming language
  val id: String

  // A short, human-readable name for the programming language
  val name: String

  // The file extensions associated with the programming language
  val extensions: Seq[String]

  // The comment string used for single-line comments in the programming language
  val commentStr: String

  // The command(s) used to execute a script written in the programming language
  val executor: Seq[String]

  // The code to parse Viash param YAML files in the programming language
  val viashParseYamlCode: String
}
