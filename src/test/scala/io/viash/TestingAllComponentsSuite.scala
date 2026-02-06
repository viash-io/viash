package io.viash

import io.viash.config.Config
import org.scalatest.funsuite.AnyFunSuite
import io.viash.helpers.{Logger, Exec, IO}
import org.scalatest.ParallelTestExecution
import io.viash.lenses.ConfigLenses
import io.viash.config.{decodeConfig, encodeConfig}
import java.nio.file.{Files, Paths}
import io.circe.parser._

class TestingAllComponentsSuite extends AnyFunSuite with ParallelTestExecution {
  Logger.UseColorOverride.value = Some(false)
  def getTestResource(path: String) = getClass.getResource(path).toString

  val tests = List(
    ("bash", "config.vsh.yaml"),
    ("python", "config.vsh.yaml"),
    ("r", "script.vsh.R"),
    ("js", "config.vsh.yaml"),
    ("scala", "config.vsh.yaml"),
    ("csharp", "config.vsh.yaml"),
    ("executable", "config.vsh.yaml")
  )

  val multiples = List(
    "boolean",
    "integer",
    "long",
    "double",
  )

  for ((name, file) <- tests) {
    val config = getTestResource(s"/test_languages/$name/$file")

    // only run testbash natively because other requirements might not be available
    if (name == "bash") {
      test(s"Testing $name engine native", NativeTest) {
        TestHelper.testMain("test", "--engine", "native", "--runner", "executable", config)
      }

      for (multiType <- multiples) {
        test(s"Testing $name engine native, multiple $multiType", NativeTest) {
          TestHelper.testMain(
            "test", "--engine", "native", "--runner", "executable", config,
            "-c", s"""<preparse>.argument_groups[.name == "Arguments"].arguments[.name == "--multiple" || .name == "multiple_pos"].type := "$multiType"""",
            "-c", s""".test_resources[.type == "bash_script"].path := "../multi-$multiType.sh""""
          )
        }
      }
    }

    test(s"Testing $name engine docker", DockerTest) {
      TestHelper.testMain("test", "--engine", "docker", "--runner", "executable", config)
    }

    if (name != "executable") {
      for (multiple <- multiples) {
        test(s"Testing $name engine docker, multiple $multiple", DockerTest) {
          TestHelper.testMain(
            "test", "--engine", "docker", "--runner", "executable", config,
            "-c", s"""<preparse>.argument_groups[.name == "Arguments"].arguments[.name == "--multiple" || .name == "multiple_pos"].type := "$multiple"""",
            "-c", s""".test_resources[.type == "bash_script"].path := "../multi-$multiple.sh""""
          )
        }
      }

      test(s"Testing $name engine docker, multiple file", DockerTest) {
        TestHelper.testMain(
          "test", "--engine", "docker", "--runner", "executable", config,
          "-c", s"""<preparse>.argument_groups[.name == "Arguments"].arguments[.name == "--multiple" || .name == "multiple_pos"].type := "file"""",
          "-c", s"""<preparse>.argument_groups[.name == "Arguments"].arguments[.name == "--multiple" || .name == "multiple_pos"].must_exist := false""",
          "-c", s""".test_resources[.type == "bash_script"].path := "../multi-file.sh""""
        )
      }
    }

    test(s"Testing $name whether yaml parsing/unparsing is invertible") {
      import io.circe.syntax._

      val conf = Config.read(config)

      // convert to json
      val confJson = conf.asJson

      // convert back to config
      val conf2 = confJson.as[Config].toOption.get

      // strip parent parameters as those are internal functionality and are not serialized
      val strippedConf1 = ConfigLenses.resourcesLens.modify(_.map(_.copyResource(parent = None)))(conf)
      val strippedConf2 = ConfigLenses.testResourcesLens.modify(_.map(_.copyResource(parent = None)))(strippedConf1)
      // check if equal
      assert(strippedConf2 == conf2)
    }

    // Test that VIASH_KEEP_WORK_DIR preserves the work directory and params.json is valid
    // Skip executable type as it doesn't have a script/language
    if (name != "executable") {
      test(s"Testing $name params.json generation with VIASH_KEEP_WORK_DIR", DockerTest) {
        val tempDir = IO.makeTemp("viash_test_json_")
        val executable = tempDir.resolve(s"test_languages_$name")
        
        try {
          // Build the component with docker engine
          TestHelper.testMain("build", "--engine", "docker", "-o", tempDir.toString, config)
          
          // Run with VIASH_KEEP_WORK_DIR set
          val result = Exec.runCatchPath(
            List(
              executable.toString,
              getClass.getResource("/testbash/resource1.txt").getPath,
              "--whole_number", "42",
              "--real_number", "3.14",
              "-s", "test_string",
              "--multiple", "a", "--multiple", "b"
            ),
            cwd = None,
            extraEnv = Seq("VIASH_KEEP_WORK_DIR" -> "1")
          )
          
          assert(result.exitValue == 0, s"Component execution failed:\n${result.output}")
          
          // Extract work directory path from output
          val workDirPattern = """Keeping work directory at '([^']+)'""".r
          val workDirMatch = workDirPattern.findFirstMatchIn(result.output)
          assert(workDirMatch.isDefined, s"Could not find work directory path in output:\n${result.output}")
          
          val workDir = Paths.get(workDirMatch.get.group(1))
          val paramsJson = workDir.resolve("params.json")
          
          // Verify params.json exists
          assert(Files.exists(paramsJson), s"params.json not found at $paramsJson")
          
          // Read and parse JSON
          val jsonContent = new String(Files.readAllBytes(paramsJson))
          val json = parse(jsonContent)
          assert(json.isRight, s"Invalid JSON:\n$jsonContent")
          
          // Verify JSON structure
          val jsonObj = json.toOption.get.asObject.get
          assert(jsonObj.contains("par"), "JSON should contain 'par' section")
          assert(jsonObj.contains("meta"), "JSON should contain 'meta' section")
          assert(jsonObj.contains("dep"), "JSON should contain 'dep' section")
          
          // Verify par section has expected values
          val par = jsonObj("par").get.asObject.get
          assert(par("whole_number").get.asNumber.get.toInt.contains(42), "whole_number should be 42")
          val realNum = par("real_number").get.asNumber.get.toDouble
          assert(math.abs(realNum - 3.14) < 0.001, s"real_number should be 3.14, got $realNum")
          assert(par("s").get.asString.contains("test_string"), "s should be 'test_string'")
          
          // Verify array parameter
          val multiple = par("multiple").get.asArray.get
          assert(multiple.length == 2, "multiple should have 2 elements")
          assert(multiple(0).asString.contains("a"), "first element should be 'a'")
          assert(multiple(1).asString.contains("b"), "second element should be 'b'")
          
          // Verify meta section
          val meta = jsonObj("meta").get.asObject.get
          assert(meta("name").get.asString.contains(s"test_languages_$name"), s"name should be test_languages_$name")
          
          // Clean up work directory
          IO.deleteRecursively(workDir)
        } finally {
          IO.deleteRecursively(tempDir)
        }
      }
    }
  }
}