package io.viash.helpers.circe

import org.scalatest.{BeforeAndAfterAll, FunSuite}
import io.circe._
import io.circe.yaml.parser
import io.viash.helpers.IO
import java.nio.file.Files

class RichJsonTest extends FunSuite with BeforeAndAfterAll {
  private val temporaryFolder = IO.makeTemp("richjson")

  test("checking whether withDefault works") {
    val json1 = Json.fromJsonObject(JsonObject("foo" -> Json.fromString("str")))

    // check whether filling default works
    val json2 = json1.withDefault("bar", Json.fromString("zzz"))
    val json2Expected = Json.fromJsonObject(JsonObject(
      "foo" -> Json.fromString("str"),
      "bar" -> Json.fromString("zzz")
    ))
    assert(json2 == json2Expected)
  }

  test("checking whether dropEmptyRecursively works") {
    val json1 = parser.parse("""
      |a: null
      |b: []
      |c: {}
      |d: []
      |""".stripMargin
    ).getOrElse(Json.Null)
    assert(json1.dropEmptyRecursively == Json.Null)

    val json2 = parser.parse("""
      |a: 10
      |b: []
      |c: {}
      |d: []
      |""".stripMargin
    ).getOrElse(Json.Null)
    val json2Expected = Json.fromJsonObject(JsonObject("a" -> Json.fromInt(10)))
    assert(json2.dropEmptyRecursively == json2Expected)

    val json3 = parser.parse("""
      |a: null
      |c:
      |  foo: aa
      |  bar: bb
      |""".stripMargin
    ).getOrElse(Json.Null)
    val json3Expected = Json.fromJsonObject(JsonObject(
      "c" -> Json.fromJsonObject(JsonObject(
        "foo" -> Json.fromString("aa"),
        "bar" -> Json.fromString("bb")
      ))
    ))
    assert(json3.dropEmptyRecursively == json3Expected)
  }

  test("checking whether concatDeepMerge works") {
    val json1 = parser.parse("""
      |a: [1, 2]
      |b:
      |  c: foo
      |  d: bar
      |""".stripMargin
    ).getOrElse(Json.Null)
    val json2 = parser.parse("""
      |a: [3, 4]
      |b:
      |  c: zing
      |  e: zoom
      |""".stripMargin
    ).getOrElse(Json.Null)
    val jsonMergeExpected = parser.parse("""
      |a: [1, 2, 3, 4]
      |b:
      |  c: zing
      |  d: bar
      |  e: zoom
      |""".stripMargin
    ).getOrElse(Json.Null)

    // check whether filling default works
    val jsonMerge = json1.concatDeepMerge(json2)
    assert(jsonMerge == jsonMergeExpected)
  }

  test("checking whether inherit works") {
    // write json to file
    IO.write("a: [3, 4]", temporaryFolder.resolve("obj1.yaml"))

    val json1 = parser.parse("""
      |__inherits__: obj1.yaml
      |a: [1, 2]
      |""".stripMargin
    ).getOrElse(Json.Null)
    val jsonExpected = parser.parse("""
      |a: [1, 2, 3, 4]
      |""".stripMargin
    ).getOrElse(Json.Null)

    // check whether filling default works
    val jsonOut = json1.inherit(temporaryFolder.toUri())
    assert(jsonOut == jsonExpected)
  }

  test("checking whether inherit without stripping the inherits works") {
    // write json to file
    val obj1Path = temporaryFolder.resolve("obj1.yaml")
    IO.write("a: [3, 4]", obj1Path)

    val json1 = parser.parse("""
      |__inherits__: obj1.yaml
      |a: [1, 2]
      |""".stripMargin
    ).getOrElse(Json.Null)
    val jsonExpected = parser.parse(s"""
      |__inherits__: file:${obj1Path}
      |a: [1, 2, 3, 4]
      |""".stripMargin
    ).getOrElse(Json.Null)

    // check whether filling default works
    val jsonOut = json1.inherit(temporaryFolder.toUri(), stripInherits = false)
    assert(jsonOut == jsonExpected)
  }

  test("checking whether multiple inheritance works") {
    IO.write("a: [3]", temporaryFolder.resolve("obj2.yaml"))
    IO.write("a: [4]", temporaryFolder.resolve("obj3.yaml"))

    val json1 = parser.parse("""
      |__inherits__: [obj2.yaml, obj3.yaml]
      |a: [1, 2]
      |""".stripMargin
    ).getOrElse(Json.Null)
    val jsonExpected = parser.parse("""
      |a: [1, 2, 3, 4]
      |""".stripMargin
    ).getOrElse(Json.Null)

    // check whether filling default works
    val jsonOut = json1.inherit(temporaryFolder.toUri())
    assert(jsonOut == jsonExpected)
  }

  test("checking whether relative files during inheritance works") {
    val dir1 = temporaryFolder.resolve("dir1")
    Files.createDirectory(dir1)
    IO.write("__inherits__: file2.yaml\na: [3]", dir1.resolve("file1.yaml"))
    IO.write("a: [4]", dir1.resolve("file2.yaml"))

    val json1 = parser.parse("""
      |__inherits__: dir1/file1.yaml
      |a: [1, 2]
      |""".stripMargin
    ).getOrElse(Json.Null)
    val jsonExpected = parser.parse("""
      |a: [1, 2, 3, 4]
      |""".stripMargin
    ).getOrElse(Json.Null)

    // check whether filling default works
    val jsonOut = json1.inherit(temporaryFolder.toUri())
    assert(jsonOut == jsonExpected)
  }

  test("checking whether stripInherits works") {
    val json1 = parser.parse("""
      |__inherits__: foo
      |a: 1
      |b:
      |  __inherits__: bar
      |  c: zzz
      |""".stripMargin
    ).getOrElse(Json.Null)

    val jsonExpected = parser.parse("""
      |a: 1
      |b:
      |  c: zzz
      |""".stripMargin
    ).getOrElse(Json.Null)
    val jsonOut = json1.stripInherits
    assert(jsonOut == jsonExpected)
  }

  override def afterAll() {
    IO.deleteRecursively(temporaryFolder)
  }
}