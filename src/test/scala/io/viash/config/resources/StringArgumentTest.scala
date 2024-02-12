package io.viash.config.resources

import org.scalatest.funsuite.AnyFunSuite
import io.viash.helpers.Logger
import io.viash.config.resources.{JavaScriptScript, CSharpScript, Executable, ScalaScript, NextflowScript, BashScript, PythonScript, RScript}

class CommandsTest extends AnyFunSuite {
  Logger.UseColorOverride.value = Some(false)

  test("Test Bash script") {
    val script = BashScript(path = Some("bar"))
    val command = script.command("foo")
    val commandSeq = script.commandSeq("foo")

    assert(command == "bash \"foo\"")
    assert(commandSeq == Seq("bash", "foo"))
  }

  test("Test CSharp script") {
    val script = CSharpScript(path = Some("bar"))
    val command = script.command("foo")
    val commandSeq = script.commandSeq("foo")

    assert(command == "dotnet script \"foo\"")
    assert(commandSeq == Seq("dotnet", "script", "foo"))
  }

  test("Test Executable") {
    val script = Executable(path = Some("bar"))
    val command = script.command("foo")
    val commandSeq = script.commandSeq("foo")

    assert(command == "foo")
    assert(commandSeq == Seq("foo"))
  }

  test("Test JavaScript script") {
    val script = JavaScriptScript(path = Some("bar"))
    val command = script.command("foo")
    val commandSeq = script.commandSeq("foo")

    assert(command == "node \"foo\"")
    assert(commandSeq == Seq("node", "foo"))
  }

  test("Test Nextflow script") {
    val script = NextflowScript(path = Some("bar"), entrypoint = "baz")
    val command = script.command("foo")
    val commandSeq = script.commandSeq("foo")

    assert(command == "nextflow run . -main-script \"foo\"")
    assert(commandSeq == Seq("nextflow", "run", ".", "-main-script", "foo"))
  }

  test("Test Python script") {
    val script = PythonScript(path = Some("bar"))
    val command = script.command("foo")
    val commandSeq = script.commandSeq("foo")

    assert(command == "python -B \"foo\"")
    assert(commandSeq == Seq("python", "-B", "foo"))
  }

  test ("Test R script") {
    val script = RScript(path = Some("bar"))
    val command = script.command("foo")
    val commandSeq = script.commandSeq("foo")

    assert(command == "Rscript \"foo\"")
    assert(commandSeq == Seq("Rscript", "foo"))
  }

  test("Test Scala script") {
    val script = ScalaScript(path = Some("bar"))
    val command = script.command("foo")
    val commandSeq = script.commandSeq("foo")

    assert(command == "scala -nc \"foo\"")
    assert(commandSeq == Seq("scala", "-nc", "foo"))
  }

}