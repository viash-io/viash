package io.viash.functionality

import org.scalatest.funsuite.AnyFunSuite
import io.viash.helpers.Logger
import io.viash.functionality.resources.{BashScript, PlainFile}

class MainScriptTest extends AnyFunSuite {
  Logger.UseColorOverride.value = Some(false)

  test("Single script") {
    val resources = List(BashScript(path = Some("foo.sh")))
    val fun = Functionality("fun", resources = resources)

    val mainScript = fun.mainScript
    val additionalResources = fun.additionalResources

    assert(mainScript.isDefined)
    assert(mainScript.get.path == Some("foo.sh"))
    assert(additionalResources.isEmpty)
  }

  test("Multiple scripts") {
    val resources = List(
      BashScript(path = Some("foo.sh")),
      BashScript(path = Some("bar.sh")),
      BashScript(path = Some("baz.sh")),
    )
    val fun = Functionality("fun", resources = resources)

    val mainScript = fun.mainScript
    val additionalResources = fun.additionalResources

    assert(mainScript.isDefined)
    assert(mainScript.get.path == Some("foo.sh"))
    assert(additionalResources.length == 2)
  }

  test("Single text file") {
    val resources = List(PlainFile(path = Some("foo.txt")))
    val fun = Functionality("fun", resources = resources)

    val mainScript = fun.mainScript
    val additionalResources = fun.additionalResources

    assert(mainScript.isEmpty)
    assert(additionalResources.length == 1)
    assert(additionalResources.head.path == Some("foo.txt"))
  }

  test("Multiple text files") {
    val resources = List(
      PlainFile(path = Some("foo.txt")),
      PlainFile(path = Some("bar.txt")),
      PlainFile(path = Some("baz.txt")),
    )
    val fun = Functionality("fun", resources = resources)

    val mainScript = fun.mainScript
    val additionalResources = fun.additionalResources

    assert(mainScript.isEmpty)
    assert(additionalResources.length == 3)
  }

  test("Mixed Script and text files, first is script") {
    val resources = List(
      BashScript(path = Some("foo.sh")),
      PlainFile(path = Some("bar.txt")),
      BashScript(path = Some("baz.sh")),
    )

    val fun = Functionality("fun", resources = resources)

    val mainScript = fun.mainScript
    val additionalResources = fun.additionalResources

    assert(mainScript.isDefined)
    assert(mainScript.get.path == Some("foo.sh"))
    assert(additionalResources.length == 2)
  }

  test("Mixed Script and text files, first is text file") {
    val resources = List(
      PlainFile(path = Some("foo.txt")),
      BashScript(path = Some("bar.sh")),
      PlainFile(path = Some("baz.txt")),
    )

    val fun = Functionality("fun", resources = resources)

    val mainScript = fun.mainScript
    val additionalResources = fun.additionalResources

    assert(mainScript.isEmpty)
    assert(additionalResources.length == 3)
  }
}