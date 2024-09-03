package io.viash.runners.nextflow

import scala.io.Source
import java.io.File
import sys.process._

object NextflowTestHelper {
  /**
    * Checks if the output contains the expected string
    *
    * @param output Output string
    * @param headerKeyword Keyword to search for
    * @param fileContentMatcher Regex to match the file content
    */
  def outputFileMatchChecker(
      output: String,
      headerKeyword: String,
      fileContentMatcher: String
  ): Unit = {
    val DebugRegex = s"$headerKeyword: \\[foo, (.*)\\]".r

    val lines = output.split("\n").find(DebugRegex.findFirstIn(_).isDefined)

    assert(lines.isDefined)
    val DebugRegex(path) = lines.get

    val src = Source.fromFile(path)
    try {
      val step3Out = src.getLines().mkString
      assert(step3Out.matches(fileContentMatcher))
    } finally {
      src.close()
    }
  }

  /**
    * Checks if the output contains the expected string
    *
    * @param output Output string
    * @param headerKeyword Keyword to search for
    * @return Returns a tuple of the id and the arguments
    */
  def outputTupleProcessor(output: String, headerKeyword: String): Array[(String, Map[String, String])] = {
    val stdOutLines = output.split("\n")

    val DebugRegex = s"$headerKeyword: \\[(.*), \\[(.*)\\]\\]".r
    val debugPrints = stdOutLines.flatMap {
      _ match {
        case DebugRegex(id, argStr) =>
          // turn argStr into Map[String, String]
          import io.circe.yaml.parser
          import io.circe.Json

          val input2 = "{" + argStr.replaceAll(":", ": ") + "}"

          val js = parser.parse(input2).toOption.get
          val js2 = js.mapObject(_.mapValues { v =>
            v match {
              case a if a.isArray =>
                Json.fromString(
                  a.asArray.get.map(_.asString.get).mkString("[", ", ", "]")
                )
              case a if a.isString => Json.fromString(a.asString.get)
              case _               => Json.fromString(v.toString)
            }
          })

          val argMap = js2.as[Map[String, String]].toOption.get

          Some((id, argMap))
        case _ => None
      }
    }

    debugPrints
  }

  trait CheckArg {
    val name: String
    def assert(id: String, args: Map[String, String]): Unit
  }

  case class MatchCheck(name: String, s: String) extends CheckArg {
    import org.scalatest.Assertions.{assert => scassert}
    def assert(id: String, args: Map[String, String]) = {
      scassert(args.contains(name), s"$id : args should contain $name")
      scassert(
        args(name).matches(s),
        s"$id : args($name): ${args(name)} should match $s"
      )
    }
  }
  case class EqualsCheck(name: String, s: String) extends CheckArg {
    import org.scalatest.Assertions.{assert => scassert}
    def assert(id: String, args: Map[String, String]) = {
      scassert(args.contains(name), s"$id : args should contain $name")
      scassert(args(name) == s, s"$id : args($name) should equal $s")
    }
  }
  
  case class NotAvailCheck(name: String) extends CheckArg {
    import org.scalatest.Assertions.{assert => scassert}
    def assert(id: String, args: Map[String, String]) = {
      scassert(!args.contains(name), s"$id : args should not contain $name")
    }
  }

  def checkDebugArgs(
      id: String,
      debugPrints: Array[(String, Map[String, String])],
      expectedValues: List[CheckArg]
  ): Unit = {
    val idDebugPrints = debugPrints.find(_._1 == id)
    assert(idDebugPrints.isDefined)
    expectedValues.foreach(_.assert(id, idDebugPrints.get._2))
  }

  /**
    * Wrapper function for running Nextflow workflows
    *
    * @param mainScript Path to the main script
    * @param args Arguments for the workflow
    * @param cwd Working directory to execute command in
    * @param entry Entrypoint for the workflow
    * @param paramsFile Params file argument (if any)
    * @param extraEnv Extra environment variables to set
    * @param quiet Whether to suppress Nextflow meta information
    * @return Returns a Tuple3 containing the exit code, stdout and stderr.
    */
  def run(
      mainScript: String,
      args: List[String],
      entry: Option[String] = None,
      paramsFile: Option[String] = None,
      extraEnv: Seq[(String, String)] = Nil,
      quiet: Boolean = false,
      cwd: File
  ): (Int, String, String) = {

    val stdOut = new StringBuilder
    val stdErr = new StringBuilder

    val command =
      "nextflow" :: 
        { if (quiet) List("-q") else Nil } ::: 
        "run" :: "." ::
        "-main-script" :: mainScript ::
        { if (entry.isDefined) List("-entry", entry.get) else Nil } :::
        { if (paramsFile.isDefined) List("-params-file", paramsFile.get) else Nil } :::
        args
    
    // fix nextflow version to certain release
    // val extraEnv_ = extraEnv :+ ("NXF_VER" -> "22.04.5")

    val exitCode = Process(command, cwd, extraEnv : _*).!(
      ProcessLogger(str => stdOut ++= s"$str\n", str => stdErr ++= s"$str\n")
    )

    (exitCode, stdOut.toString, stdErr.toString)
  }
}
