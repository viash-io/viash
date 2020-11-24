package com.dataintuitive.viash

import org.scalatest.FunSuite

class ComponentE2ETests extends FunSuite {
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
    test(s"Testing $name platform native", NativeTest) {
      TestHelper.testMain(Array("test", "-p", "native", getTestResource(s"/$name/$file")))
    }
    test(s"Testing $name platform docker", DockerTest) {
      TestHelper.testMain(Array("test", "-p", "docker", getTestResource(s"/$name/$file")))
    }
  }
}