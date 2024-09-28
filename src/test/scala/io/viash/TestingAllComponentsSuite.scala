package io.viash

import io.viash.config.Config
import org.scalatest.funsuite.AnyFunSuite
import io.viash.helpers.Logger
import org.scalatest.ParallelTestExecution
import io.viash.lenses.ConfigLenses
import io.viash.config.{decodeConfig, encodeConfig}

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
  }
}