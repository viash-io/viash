package io.viash.config.arguments

import org.scalatest.funsuite.AnyFunSuite

import io.viash.helpers.Logger
import io.viash.helpers.circe.Convert
import io.viash.config.Config

class MergingTest extends AnyFunSuite {
  Logger.UseColorOverride.value = Some(false)

  test("No arguments or argument_groups should still parse") {
    val yaml = 
      """
        |name: foo
        |""".stripMargin

    val json = Convert.textToJson(yaml, "")
    val conf = Convert.jsonToClass[Config](json, "")

    assert(conf.name == "foo")
    assert(conf.argument_groups.isEmpty)
    assert(conf.allArguments.isEmpty)
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
    val conf = Convert.jsonToClass[Config](json, "")

    assert(conf.name == "foo")
    
    assert(conf.argument_groups.length == 1)
    assert(conf.argument_groups.head.name == "Arguments")
    assert(conf.argument_groups.head.arguments.size == 1)
    assert(conf.argument_groups.head.arguments.head.name == "bar")

    assert(conf.allArguments.size == 1)
    assert(conf.allArguments.head.name == "bar")
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
    val conf = Convert.jsonToClass[Config](json, "")

    assert(conf.name == "foo")
    
    assert(conf.argument_groups.length == 1)
    assert(conf.argument_groups.head.name == "bar")
    assert(conf.argument_groups.head.arguments.size == 1)
    assert(conf.argument_groups.head.arguments.head.name == "baz")

    assert(conf.allArguments.size == 1)
    assert(conf.allArguments.head.name == "baz")
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
    val conf = Convert.jsonToClass[Config](json, "")

    assert(conf.name == "foo")
    
    assert(conf.argument_groups.length == 2)
    assert(conf.argument_groups.head.name == "Arguments")
    assert(conf.argument_groups.head.arguments.size == 1)
    assert(conf.argument_groups.head.arguments.head.name == "bar")
    assert(conf.argument_groups(1).name == "baz")
    assert(conf.argument_groups(1).arguments.size == 1)
    assert(conf.argument_groups(1).arguments.head.name == "qux")

    assert(conf.allArguments.size == 2)
    assert(conf.allArguments.head.name == "bar")
    assert(conf.allArguments(1).name == "qux")
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
    val conf = Convert.jsonToClass[Config](json, "")

    assert(conf.name == "foo")
    
    assert(conf.argument_groups.length == 2)
    assert(conf.argument_groups.head.name == "baz")
    assert(conf.argument_groups.head.arguments.size == 1)
    assert(conf.argument_groups.head.arguments.head.name == "qux")
    assert(conf.argument_groups(1).name == "Arguments")
    assert(conf.argument_groups(1).arguments.size == 1)
    assert(conf.argument_groups(1).arguments.head.name == "bar")

    assert(conf.allArguments.size == 2)
    assert(conf.allArguments.head.name == "qux")
    assert(conf.allArguments(1).name == "bar")
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
    val conf = Convert.jsonToClass[Config](json, "")

    assert(conf.name == "foo")
    
    assert(conf.argument_groups.length == 1)
    assert(conf.argument_groups.head.name == "Arguments")
    assert(conf.argument_groups.head.arguments.size == 2)
    assert(conf.argument_groups.head.arguments.head.name == "bar")
    assert(conf.argument_groups.head.arguments(1).name == "baz")
    assert(conf.allArguments.size == 2)
    assert(conf.allArguments.head.name == "bar")
    assert(conf.allArguments(1).name == "baz")
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
    val conf = Convert.jsonToClass[Config](json, "")

    assert(conf.name == "foo")
    
    assert(conf.argument_groups.length == 1)
    assert(conf.argument_groups.head.name == "Arguments")
    assert(conf.argument_groups.head.arguments.size == 2)
    assert(conf.argument_groups.head.arguments.head.name == "baz")
    assert(conf.argument_groups.head.arguments(1).name == "bar")
    assert(conf.allArguments.size == 2)
    assert(conf.allArguments.head.name == "baz")
    assert(conf.allArguments(1).name == "bar")
  }
  
}
