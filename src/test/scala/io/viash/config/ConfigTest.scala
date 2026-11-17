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
import io.viash.ConfigDeriver

class ConfigTest extends AnyFunSuite with BeforeAndAfterAll {
  Logger.UseColorOverride.value = Some(false)

  private val temporaryFolder = IO.makeTemp(s"viash_${this.getClass.getName}_")
  private val tempFolStr = temporaryFolder.toString

  private val nsPath = getClass.getResource("/testns/").getPath

  private val configFile = getClass.getResource(s"/testbash/config.vsh.yaml").getPath
  private val temporaryConfigFolder = IO.makeTemp(s"viash_${this.getClass.getName}_")
  private val configDeriver = ConfigDeriver(Paths.get(configFile), temporaryConfigFolder)
  
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

  test("Can find and read multiple sources with an empty config amongst them") {
    val tempFolder = temporaryFolder.resolve("test3")
    Files.createDirectory(tempFolder)
    IO.copyFolder(nsPath, tempFolder.toString())

    // an empty (or comment-only) config should not stop the rest of the namespace from being read
    Files.createDirectories(tempFolder.resolve("src/ns_empty"))
    IO.write("# name: ns_empty\n", tempFolder.resolve("src/ns_empty/config.vsh.yaml"))

    val configs = Config.readConfigs(tempFolder.toString())

    assert(configs.length == 8)
    assert(configs.filter(_.status == None).length == 5)
    assert(configs.filter(_.status == Some(status.Disabled)).length == 1, "Expect 1 disabled component")
    assert(configs.filter(_.status == Some(status.ParseError)).length == 2, "Expect 2 failed components: the pre-existing one and the empty one")
  }

  test("Reading a comment-only config file throws a clean, dedicated error") {
    val tempFolder = temporaryFolder.resolve("test_empty_config")
    Files.createDirectory(tempFolder)
    val configPath = tempFolder.resolve("config.vsh.yaml")
    // contains "name:" (in a comment) so it passes the yaml/script detection in readYAML,
    // but resolves to an empty yaml document (circe-yaml parses this as Json.False)
    IO.write("# name: this_is_just_a_comment\n", configPath)

    val ex = intercept[io.viash.exceptions.ConfigParserEmptyException] {
      Config.read(configPath.toString)
    }
    assert(ex.getMessage() == "The Viash config file is empty.")
  }

  test("Reading a blank config file throws a clean, dedicated error") {
    val tempFolder = temporaryFolder.resolve("test_blank_config")
    Files.createDirectory(tempFolder)
    val configPath = tempFolder.resolve("config.vsh.yaml")
    // no "name:" anywhere, so it doesn't hit the same detection path as the comment-only case above
    IO.write("   \n\n", configPath)

    val ex = intercept[io.viash.exceptions.ConfigParserEmptyException] {
      Config.read(configPath.toString)
    }
    assert(ex.getMessage() == "The Viash config file is empty.")
  }

  private def writeResourcesConfig(folderName: String, resourcesYaml: String): String = {
    val tempFolder = temporaryFolder.resolve(folderName)
    Files.createDirectory(tempFolder)
    val configPath = tempFolder.resolve("config.vsh.yaml")
    IO.write(
      s"""name: $folderName
         |resources:
         |  - type: bash_script
         |    text: echo hi
         |$resourcesYaml""".stripMargin,
      configPath
    )
    configPath.toString
  }

  test("Two inline text resources with the same dest throw a config error") {
    val configPath = writeResourcesConfig("test_dup_dest",
      """  - type: file
        |    dest: notes.txt
        |    text: hello
        |  - type: file
        |    dest: notes.txt
        |    text: something else
        |""".stripMargin)

    val ex = intercept[io.viash.exceptions.ConfigResourceConflictException] {
      Config.read(configPath)
    }
    assert(ex.getMessage().contains("Multiple resources have destination 'notes.txt'"))
  }

  test("Two inline text resources with the same filename in different folders end up in the meta config") {
    val configPath = writeResourcesConfig("test_dup_filename",
      """  - type: file
        |    dest: a/notes.txt
        |    text: hello
        |  - type: file
        |    dest: b/notes.txt
        |    text: something else
        |""".stripMargin)

    val config = Config.read(configPath)
    val metaText = ConfigMeta.toMetaFile(AppliedConfig(config, None, Nil, None), None).text.get

    assert(!metaText.contains("VIASH_PLACEHOLDER"), "All placeholders should be replaced")
    assert(metaText.contains("dest: \"a/notes.txt\""))
    assert(metaText.contains("dest: \"b/notes.txt\""))
    assert(metaText.contains("hello"))
    assert(metaText.contains("something else"))
  }

  test("A path resource and an inline text resource with the same dest throw a config error") {
    val configPath = writeResourcesConfig("test_dup_path_text",
      """  - type: file
        |    path: notes.txt
        |  - type: file
        |    dest: ./notes.txt
        |    text: hello
        |""".stripMargin)

    val ex = intercept[io.viash.exceptions.ConfigResourceConflictException] {
      Config.read(configPath)
    }
    assert(ex.getMessage().contains("Multiple resources have destination 'notes.txt'"))
  }

  test("Identical duplicate resources are allowed") {
    val configPath = writeResourcesConfig("test_identical_resources",
      """  - type: file
        |    dest: notes.txt
        |    text: hello
        |  - type: file
        |    dest: notes.txt
        |    text: hello
        |""".stripMargin)

    val config = Config.read(configPath)
    assert(config.resources.count(_.dest == Some("notes.txt")) == 2)

    val metaText = ConfigMeta.toMetaFile(AppliedConfig(config, None, Nil, None), None).text.get
    assert(!metaText.contains("VIASH_PLACEHOLDER"), "All placeholders should be replaced")
  }

  test("Test default scope value") {
    val newConfigFilePath = configDeriver.derive(Nil, "default_scope")
    val newConfig = Config.read(newConfigFilePath)

    assert(newConfig.scope.isRight)
    val scope = newConfig.scope.toOption.get
    assert(scope.image == ScopeEnum.Public)
    assert(scope.target == ScopeEnum.Public)
  }

  test("Test public scope value") {
    val newConfigFilePath = configDeriver.derive(""".scope := "public"""", "public_scope")
    val newConfig = Config.read(newConfigFilePath)

    assert(newConfig.scope.isRight)
    val scope = newConfig.scope.toOption.get
    assert(scope.image == ScopeEnum.Public)
    assert(scope.target == ScopeEnum.Public)
  }

  test("Test private scope value") {
    val newConfigFilePath = configDeriver.derive(""".scope := "private"""", "private_scope")
    val newConfig = Config.read(newConfigFilePath)

    assert(newConfig.scope.isRight)
    val scope = newConfig.scope.toOption.get
    assert(scope.image == ScopeEnum.Private)
    assert(scope.target == ScopeEnum.Private)
  }

  test("Test test scope value") {
    val newConfigFilePath = configDeriver.derive(""".scope := "test"""", "test_scope")
    val newConfig = Config.read(newConfigFilePath)

    assert(newConfig.scope.isRight)
    val scope = newConfig.scope.toOption.get
    assert(scope.image == ScopeEnum.Test)
    assert(scope.target == ScopeEnum.Test)
  }

  test("Test scope value with different image and target") {
    val newConfigFilePath = configDeriver.derive(""".scope := {image: "test", target: "private"}""", "custom_scope")
    val newConfig = Config.read(newConfigFilePath)

    assert(newConfig.scope.isRight)
    val scope = newConfig.scope.toOption.get
    assert(scope.image == ScopeEnum.Test)
    assert(scope.target == ScopeEnum.Private)
  }

  override def afterAll(): Unit = {
    IO.deleteRecursively(temporaryFolder)
    IO.deleteRecursively(temporaryConfigFolder)
  }
}