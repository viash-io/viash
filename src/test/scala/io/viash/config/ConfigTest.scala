package io.viash.config

import org.scalatest.BeforeAndAfterAll
import org.scalatest.funsuite.AnyFunSuite
import java.nio.file.{Files, Paths, StandardCopyOption}
import scala.util.Try
import io.circe._
import io.circe.yaml.{parser => YamlParser}
import io.circe.syntax._
import io.viash.helpers.circe._
import io.viash.helpers.data_structures._
import io.viash.helpers.Logger
import io.viash.engines.NativeEngine
import io.viash.runners.ExecutableRunner
import io.viash.helpers.IO
import io.viash.helpers.status

class ConfigTest extends AnyFunSuite with BeforeAndAfterAll {
  Logger.UseColorOverride.value = Some(false)

  private val temporaryFolder = IO.makeTemp(s"viash_${this.getClass.getName}_")
  private val tempFolStr = temporaryFolder.toString

  private val nsPath = getClass.getResource("/testns/").getPath
  
  val infoJson = Yaml("""
    |foo:
    |  bar:
    |    baz:
    |      10
    |arg: aaa
    |""".stripMargin)

  test("Simple getters and helper functions") {
    val conf = Config(name = "foo")

    assert(conf.name == "foo")
    assert(conf.description == None)
    assert(conf.info == Json.Null)

    val confParsed = conf.asJson.as[Config].fold(throw _, a => a)
    assert(confParsed == conf)
  }

  test("Simple getters and helper functions on object with many non-default values") {
    val conf = Config(
      name = "one_two_three_four",
      description = Some("foo"),
      info = infoJson
    )

    assert(conf.name == "one_two_three_four")
    assert(conf.description == Some("foo"))
    assert(conf.info == infoJson)
    
    val confParsed = conf.asJson.as[Config].fold(throw _, a => a)
    assert(confParsed == conf)
  }

  test("GetEngines and GetRunners with some engines and runners") {
    val conf = Config(
      name = "foo",
      engines = List(
        NativeEngine("engine0"),
        NativeEngine("engine1"),
        NativeEngine("engine2")
      ),
      runners = List(
        ExecutableRunner("runner0"),
        ExecutableRunner("runner1"),
        ExecutableRunner("runner2")
      )
    )

    assert(conf.findEngines(None) == conf.engines)
    assert(conf.findRunners(None) == conf.runners)

    assert(conf.findEngines(Some(".*")) == conf.engines)
    assert(conf.findRunners(Some(".*")) == conf.runners)

    assert(conf.findEngines(Some("engine0")) == List(NativeEngine("engine0")))
    assert(conf.findEngines(Some("engine1")) == List(NativeEngine("engine1")))
    assert(conf.findRunners(Some("runner0")) == List(ExecutableRunner("runner0")))
    assert(conf.findRunners(Some("runner1")) == List(ExecutableRunner("runner1")))

    assert(conf.findEngines(Some(".*0")) == List(NativeEngine("engine0")))
    assert(conf.findEngines(Some(".*1")) == List(NativeEngine("engine1")))
    assert(conf.findRunners(Some(".*0")) == List(ExecutableRunner("runner0")))
    assert(conf.findRunners(Some(".*1")) == List(ExecutableRunner("runner1")))
  }

  test("GetEngines and GetRunners without engines or runners") {
    val conf = Config(name = "foo")

    assert(conf.findEngines(None) == List(NativeEngine("native")))
    assert(conf.findRunners(None) == List(ExecutableRunner("executable")))

    assert(conf.findEngines(Some(".*")) == List(NativeEngine("native")))
    assert(conf.findRunners(Some(".*")) == List(ExecutableRunner("executable")))

    assert(conf.findEngines(Some("native")) == List(NativeEngine("native")))
    assert(conf.findRunners(Some("executable")) == List(ExecutableRunner("executable")))
  }

  test("Can find and read multiple sources with default testns components") {
    val tempFolder = temporaryFolder.resolve("test1")
    Files.createDirectory(tempFolder)
    IO.copyFolder(nsPath, tempFolder.toString())

    val configs = Config.readConfigs(tempFolder.toString())

    assert(configs.length == 7)
    assert(configs.filter(_.status == None).length == 5)
    assert(configs.filter(_.status == Some(status.Disabled)).length == 1, "Expect 1 disabled component")
    assert(configs.filter(_.status == Some(status.ParseError)).length == 1, "Expect 1 failed component")
  }

  test("Can find and read multiple sources with multiple failing components") {
    val tempFolder = temporaryFolder.resolve("test2")
    Files.createDirectory(tempFolder)
    IO.copyFolder(nsPath, tempFolder.toString())

    Files.createDirectories(tempFolder.resolve("src/ns_error2"))
    IO.write("this is an invalid config", tempFolder.resolve("src/ns_error2/invalid_config.vsh.yaml"))

    val configs = Config.readConfigs(tempFolder.toString())

    assert(configs.length == 8)
    assert(configs.filter(_.status == None).length == 5)
    assert(configs.filter(_.status == Some(status.Disabled)).length == 1, "Expect 1 disabled component")
    assert(configs.filter(_.status == Some(status.ParseError)).length == 2, "Expect 2 failed component")
  }

  // TODO: expand functionality tests

  override def afterAll(): Unit = {
    IO.deleteRecursively(temporaryFolder)
  }
}