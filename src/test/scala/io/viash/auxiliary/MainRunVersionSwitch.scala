package io.viash.auxiliary

import io.viash.{NativeTest, TestHelper, Main}
import io.viash.helpers.{IO, SysEnv, Logger}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.funsuite.AnyFunSuite

import java.nio.file.{Files, Paths, StandardCopyOption}
import java.io.ByteArrayOutputStream
import io.viash.exceptions.ExitException

class MainRunVersionSwitch extends AnyFunSuite with BeforeAndAfterAll {
  Logger.UseColorOverride.value = Some(false)

  test("Verify VIASH_VERSION is unset") {
    assert(SysEnv.viashVersion.isEmpty)
  }

  test("Can override variables") {
    SysEnv.set("VIASH_VERSION", "foo")
    assert(SysEnv.viashVersion == Some("foo"))
  }

  test("Can erase variables") {
    SysEnv.remove("VIASH_VERSION")
    assert(SysEnv.viashVersion.isEmpty)
  }

  test("Check version without specifying the version to run", NativeTest) {

    val arguments = Seq("--version")
    val outStream = new ByteArrayOutputStream()
    val errStream = new ByteArrayOutputStream()
    Console.withOut(outStream) {
      Console.withErr(errStream) {
        assertThrows[ExitException] {
          Main.mainCLIOrVersioned(arguments.toArray)
        }
      }
    }
    
    val stdout = outStream.toString
    val stderr = errStream.toString

    assert(stdout.contains("viash test (c) 2020 Data Intuitive"))
    assert(stderr.isEmpty())
  }

  test("Check version with specifying the version to run", NativeTest) {

    SysEnv.set("VIASH_VERSION", "0.6.6")

    val version = SysEnv.viashVersion
    assert(version == Some("0.6.6"))

    val arguments = Seq("--version")
    val outStream = new ByteArrayOutputStream()
    val errStream = new ByteArrayOutputStream()
    Console.withOut(outStream) {
      Console.withErr(errStream) {
        Main.mainCLIOrVersioned(arguments.toArray)
      }
    }
    
    val stdout = outStream.toString
    val stderr = errStream.toString

    assert(stdout.contains("viash 0.6.6 (c) 2020 Data Intuitive"))
    assert(stderr.isEmpty())
  }

  test("Check version with specifying '-' as the version to run", NativeTest) {

    SysEnv.set("VIASH_VERSION", "-")

    val version = SysEnv.viashVersion
    assert(version == Some("-"))

    val arguments = Seq("--version")
    val outStream = new ByteArrayOutputStream()
    val errStream = new ByteArrayOutputStream()
    Console.withOut(outStream) {
      Console.withErr(errStream) {
        assertThrows[ExitException] {
          Main.mainCLIOrVersioned(arguments.toArray)
        }
      }
    }
    
    val stdout = outStream.toString
    val stderr = errStream.toString

    assert(stdout.contains("viash test (c) 2020 Data Intuitive"))
    assert(stderr.isEmpty())
  }

  test("Check version with specifying an invalid version", NativeTest) {

    // remove the 'invalid' viash version if it already exists
    val path = Paths.get(SysEnv.viashHome).resolve("releases").resolve("invalid").resolve("viash")
    Files.deleteIfExists(path)

    SysEnv.set("VIASH_VERSION", "invalid")

    val version = SysEnv.viashVersion
    assert(version == Some("invalid"))

    val arguments = Seq("--version")
    val outStream = new ByteArrayOutputStream()
    val errStream = new ByteArrayOutputStream()
    val caught = intercept[RuntimeException] {
      Console.withOut(outStream) {
        Console.withErr(errStream) {
          Main.mainCLIOrVersioned(arguments.toArray)
        }
      }
    }
    
    val stdout = outStream.toString
    val stderr = errStream.toString

    assert(stdout.isEmpty())
    assert(stderr.isEmpty())
    assert(caught.getMessage().contains("Could not download file: https://github.com/viash-io/viash/releases/download/invalid/viash"))
  }

  override def afterAll(): Unit = {
    SysEnv.remove("VIASH_VERSION")
  }

}
