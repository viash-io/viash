package com.dataintuitive.viash

import org.scalatest.FunSuite

class ComponentE2ETests extends FunSuite {
  def getTestResource(path: String) = getClass.getResource(path).toString

  test(s"Testing testbash platform native", NativeTest) {
    TestHelper.testMain(Array("test", "-P", "native", "-f", getTestResource("/testbash/functionality.yaml")))
  }
  test(s"Testing testbash platform docker", DockerTest) {
    TestHelper.testMain(Array("test", "-P", "docker", "-f", getTestResource("/testbash/functionality.yaml")))
  }
  test(s"Testing testpython platform native", NativeTest) {
    TestHelper.testMain(Array("test", "-P", "native", getTestResource("/testpython/config.vsh.yaml")))
  }
  test(s"Testing testpython platform docker", DockerTest) {
    TestHelper.testMain(Array("test", "-P", "docker", getTestResource("/testpython/config.vsh.yaml")))
  }
  test(s"Testing testr platform native", NativeTest) {
    TestHelper.testMain(Array("test", "-P", "native", getTestResource("/testr/code.vsh.R")))
  }
  test(s"Testing testr platform docker", DockerTest) {
    TestHelper.testMain(Array("test", "-P", "docker", getTestResource("/testr/code.vsh.R")))
  }
  test(s"Testing testjs platform native", NativeTest) {
    TestHelper.testMain(Array("test", "-P", "native", getTestResource("/testjs/config.vsh.yaml")))
  }
  test(s"Testing testjs platform docker", DockerTest) {
    TestHelper.testMain(Array("test", "-P", "docker", getTestResource("/testjs/config.vsh.yaml")))
  }
// can't expect this executable to be available on the host. should use a different executable, perhaps?
//  test(s"Testing testexecutable platform native", NativeTest) {
//    TestHelper.executeMainAndCaptureStdOut(Array("test", "-P", "native", "-f", testResource("/testexecutable/functionality.yaml")))
//  }
  test(s"Testing testexecutable platform docker", DockerTest) {
    TestHelper.testMain(Array("test", "-P", "docker", "-f", getTestResource("/testexecutable/functionality.yaml")))
  }
}