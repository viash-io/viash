package io.viash.helpers

import org.scalatest.BeforeAndAfterAll
import org.scalatest.funsuite.AnyFunSuite
import java.nio.file.{Files, Paths, StandardCopyOption}
import scala.util.Try

class ExecTest extends AnyFunSuite with BeforeAndAfterAll {
  test("Check exec.run") {
    val execRun = Exec.run(List("echo", "hi"))
    assert(execRun.trim == "hi")

    val execRun2 = Try{Exec.run(List("eeeeeeeeee", "hi"))}.toOption
    assert(execRun2.isEmpty)

    val rootDir = Paths.get(getClass.getResource("/io/viash/").toURI())
    val execRun3 = Exec.run(List("ls"), Some(rootDir.toFile))
    assert(execRun3.contains("TestHelper.class"))

    val execRun4 = Exec.runPath(List("ls"), Some(rootDir))
    assert(execRun4.contains("TestHelper.class"))
  }

  test("Check exec.runCatch") {
    val execRun = Exec.runCatch(List("echo", "hi"))
    assert(execRun.exitValue == 0)
    assert(execRun.output.trim == "hi")

    val execRun2 = Exec.runCatch(List("eeeeeeeeee", "hi"))
    assert(execRun2.exitValue > 0)
    assert(execRun2.output.contains("No such file or directory"))

    val execRun2b = Exec.runCatch(List("cat", "thisfiledoesnotexist"))
    assert(execRun2b.exitValue > 0)
    assert(execRun2b.output.contains("No such file or directory"))

    val rootDir = Paths.get(getClass.getResource("/io/viash/").toURI())
    val execRun3 = Exec.runCatch(List("ls"), Some(rootDir.toFile))
    assert(execRun3.exitValue == 0)
    assert(execRun3.output.contains("TestHelper.class"))

    val execRun4 = Exec.runCatchPath(List("ls"), Some(rootDir))
    assert(execRun3.exitValue == 0)
    assert(execRun3.output.contains("TestHelper.class"))
  }

  test("Check exec.runOpt") {
    val execRun = Exec.runOpt(List("echo", "hi"))
    assert(execRun.isDefined)
    assert(execRun.get.trim == "hi")

    val execRun2 = Exec.runOpt(List("eeeeeeeeee", "hi"))
    assert(execRun2.isEmpty)

    val rootDir = Paths.get(getClass.getResource("/io/viash/").toURI())
    val execRun3 = Exec.runOpt(List("ls"), Some(rootDir.toFile))
    assert(execRun3.isDefined)
    assert(execRun3.get.contains("TestHelper.class"))

    val execRun4 = Exec.runOptPath(List("ls"), Some(rootDir))
    assert(execRun3.isDefined)
    assert(execRun3.get.contains("TestHelper.class"))
  }
}