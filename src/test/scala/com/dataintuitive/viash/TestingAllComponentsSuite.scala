package com.dataintuitive.viash

import com.dataintuitive.viash.config.Config
import org.scalatest.FunSuite

class TestingAllComponentsSuite extends FunSuite {
  def getTestResource(path: String) = getClass.getResource(path).toString

  val tests = List(
    ("testbash", "config.vsh.yaml"),
    ("testpython", "config.vsh.yaml"),
    ("testr", "script.vsh.R"),
    ("testjs", "config.vsh.yaml"),
    ("testscala", "config.vsh.yaml"),
    ("testexecutable", "config.vsh.yaml")
  )

  for ((name, file) ← tests) {
    val config = getTestResource(s"/$name/$file")

    test(s"Testing $name platform native", NativeTest) {
      TestHelper.testMain("test", "-p", "native", config)
    }

    test(s"Testing $name platform docker", DockerTest) {
      TestHelper.testMain("test", "-p", "docker", config)
    }

    test(s"Testing $name whether yaml parsing/unparsing is invertible") {
      import io.circe.syntax._

      val conf = Config.read(config)

      // convert to json
      val confJson = conf.asJson

      // convert back to config
      val conf2 = confJson.as[Config].right.get

      // check if equal
      assert(conf == conf2)
    }
  }
}