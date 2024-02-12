package io.viash.functionality.arguments

import org.scalatest.funsuite.AnyFunSuite

import io.viash.helpers.Logger
import io.viash.helpers.circe.Convert
import io.viash.functionality.Config

class MergingTest extends AnyFunSuite {
  Logger.UseColorOverride.value = Some(false)

  test("No arguments or argument_groups should still parse") {
    val yaml = 
      """
        |name: foo
        |""".stripMargin

    val json = Convert.textToJson(yaml, "")
    val fun = Convert.jsonToClass[Config](json, "")

    assert(fun.name == "foo")
    assert(fun.argument_groups.isEmpty)
    assert(fun.allArguments.isEmpty)
  }

  test("Only arguments should parse") {
    val yaml = 
      """
        |name: foo
        |arguments:
        |  - name: bar
        |    type: string
        |""".stripMargin

    val json = Convert.textToJson(yaml, "")
    val fun = Convert.jsonToClass[Config](json, "")

    assert(fun.name == "foo")
    
    assert(fun.argument_groups.length == 1)
    assert(fun.argument_groups.head.name == "Arguments")
    assert(fun.argument_groups.head.arguments.size == 1)
    assert(fun.argument_groups.head.arguments.head.name == "bar")

    assert(fun.allArguments.size == 1)
    assert(fun.allArguments.head.name == "bar")
  }

  test("Only argument_groups should parse") {
    val yaml = 
      """
        |name: foo
        |argument_groups:
        |  - name: bar
        |    arguments:
        |      - name: baz
        |        type: string
        |""".stripMargin

    val json = Convert.textToJson(yaml, "")
    val fun = Convert.jsonToClass[Config](json, "")

    assert(fun.name == "foo")
    
    assert(fun.argument_groups.length == 1)
    assert(fun.argument_groups.head.name == "bar")
    assert(fun.argument_groups.head.arguments.size == 1)
    assert(fun.argument_groups.head.arguments.head.name == "baz")

    assert(fun.allArguments.size == 1)
    assert(fun.allArguments.head.name == "baz")
  }

  test("Both arguments and argument_groups, arguments is first") {
    val yaml = 
      """
        |name: foo
        |arguments:
        |  - name: bar
        |    type: string
        |argument_groups:
        |  - name: baz
        |    arguments:
        |      - name: qux
        |        type: string
        |""".stripMargin

    val json = Convert.textToJson(yaml, "")
    val fun = Convert.jsonToClass[Config](json, "")

    assert(fun.name == "foo")
    
    assert(fun.argument_groups.length == 2)
    assert(fun.argument_groups.head.name == "Arguments")
    assert(fun.argument_groups.head.arguments.size == 1)
    assert(fun.argument_groups.head.arguments.head.name == "bar")
    assert(fun.argument_groups(1).name == "baz")
    assert(fun.argument_groups(1).arguments.size == 1)
    assert(fun.argument_groups(1).arguments.head.name == "qux")

    assert(fun.allArguments.size == 2)
    assert(fun.allArguments.head.name == "bar")
    assert(fun.allArguments(1).name == "qux")
  }

  test("Both arguments and argument_groups, argument_groups is first") {
    val yaml = 
      """
        |name: foo
        |argument_groups:
        |  - name: baz
        |    arguments:
        |      - name: qux
        |        type: string
        |arguments:
        |  - name: bar
        |    type: string
        |""".stripMargin

    val json = Convert.textToJson(yaml, "")
    val fun = Convert.jsonToClass[Config](json, "")

    assert(fun.name == "foo")
    
    assert(fun.argument_groups.length == 2)
    assert(fun.argument_groups.head.name == "baz")
    assert(fun.argument_groups.head.arguments.size == 1)
    assert(fun.argument_groups.head.arguments.head.name == "qux")
    assert(fun.argument_groups(1).name == "Arguments")
    assert(fun.argument_groups(1).arguments.size == 1)
    assert(fun.argument_groups(1).arguments.head.name == "bar")

    assert(fun.allArguments.size == 2)
    assert(fun.allArguments.head.name == "qux")
    assert(fun.allArguments(1).name == "bar")
  }

  test("Both arguments and argument_groups, arguments is first and defines the group as 'Arguments'") {
    val yaml = 
      """
        |name: foo
        |arguments:
        |  - name: bar
        |    type: string
        |argument_groups:
        |  - name: Arguments
        |    arguments:
        |      - name: baz
        |        type: string
        |""".stripMargin

    val json = Convert.textToJson(yaml, "")
    val fun = Convert.jsonToClass[Config](json, "")

    assert(fun.name == "foo")
    
    assert(fun.argument_groups.length == 1)
    assert(fun.argument_groups.head.name == "Arguments")
    assert(fun.argument_groups.head.arguments.size == 2)
    assert(fun.argument_groups.head.arguments.head.name == "bar")
    assert(fun.argument_groups.head.arguments(1).name == "baz")
    assert(fun.allArguments.size == 2)
    assert(fun.allArguments.head.name == "bar")
    assert(fun.allArguments(1).name == "baz")
  }

  test("Both arguments and argument_groups, argument_groups is first and defines the group as 'Arguments'") {
    val yaml = 
      """
        |name: foo
        |argument_groups:
        |  - name: Arguments
        |    arguments:
        |      - name: baz
        |        type: string
        |arguments:
        |  - name: bar
        |    type: string
        |""".stripMargin

    val json = Convert.textToJson(yaml, "")
    val fun = Convert.jsonToClass[Config](json, "")

    assert(fun.name == "foo")
    
    assert(fun.argument_groups.length == 1)
    assert(fun.argument_groups.head.name == "Arguments")
    assert(fun.argument_groups.head.arguments.size == 2)
    assert(fun.argument_groups.head.arguments.head.name == "baz")
    assert(fun.argument_groups.head.arguments(1).name == "bar")
    assert(fun.allArguments.size == 2)
    assert(fun.allArguments.head.name == "baz")
    assert(fun.allArguments(1).name == "bar")
  }
  
}
