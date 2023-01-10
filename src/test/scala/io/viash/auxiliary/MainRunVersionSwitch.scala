package io.viash.auxiliary

import io.viash.{NativeTest, TestHelper, Main}
import io.viash.helpers.IO
import org.scalatest.{BeforeAndAfterAll, FunSuite}

import java.nio.file.{Files, Paths, StandardCopyOption}
import scala.reflect.io.Directory
import java.io.ByteArrayOutputStream
import java.security.Permission

// Use SecurityManager to capture System.exit codes set by Scallop as this would cancel our testbench
sealed case class ExitException(status: Int) extends SecurityException("System.exit() is not allowed") {
}

sealed class NoExitSecurityManager extends SecurityManager {
  override def checkPermission(perm: Permission): Unit = {}

  override def checkPermission(perm: Permission, context: Object): Unit = {}

  override def checkExit(status: Int): Unit = {
    super.checkExit(status)
    throw ExitException(status)
  }
}

class MainRunVersionSwitch extends FunSuite with BeforeAndAfterAll {

  override def beforeAll(): Unit = System.setSecurityManager(new NoExitSecurityManager())

  override def afterAll(): Unit = System.setSecurityManager(null)

  def setEnv(key: String, value: String) = {
    val field = System.getenv().getClass.getDeclaredField("m")
    field.setAccessible(true)
    val map = field.get(System.getenv()).asInstanceOf[java.util.Map[java.lang.String, java.lang.String]]
    map.put(key, value)
  }

  test("Verify VIASH_VERSION is undefined by default", NativeTest) {
    assert(sys.env.get("VIASH_VERSION").isDefined == false)
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

    setEnv("VIASH_VERSION", "0.6.6")

    val version = sys.env.get("VIASH_VERSION")
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

    setEnv("VIASH_VERSION", "-")

    val version = sys.env.get("VIASH_VERSION")
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
    val path = Main.viashHome.resolve("releases").resolve("invalid").resolve("viash")
    Files.deleteIfExists(path)

    setEnv("VIASH_VERSION", "invalid")

    val version = sys.env.get("VIASH_VERSION")
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

}
