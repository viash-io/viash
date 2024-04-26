package io.viash.helpers

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.BeforeAndAfter
import java.nio.file.{Files, Path}
import java.net.URI
import io.viash.helpers.{IO, Logger}
import scala.util.Try

class IOTest extends AnyFunSuite with BeforeAndAfter {
  Logger.UseColorOverride.value = Some(false)
  var tempDir: Path = _

  before {
    tempDir = IO.makeTemp("test")
  }

  after {
    IO.deleteRecursively(tempDir)
  }

  test("makeTemp and deleteRecursively") {
    val temp = IO.makeTemp("foo")
    assert(Files.exists(temp) && Files.isDirectory(temp))
    assert(temp.toString.matches(".*foo[\\w]+"), "Temporary directory name should be randomized, strategy can differ between platforms.")

    IO.deleteRecursively(temp)
    assert(!Files.exists(temp))
  }

  test("makeTemp with addRandomized disabled, folder exists") {
    val tempDir = IO.makeTemp("foo")
    val tempDirStr = tempDir.getFileName().toString()
    val newTemp = IO.makeTemp(tempDirStr, addRandomized = false)
    assert(Files.exists(newTemp) && Files.isDirectory(newTemp))

    assert(newTemp == tempDir)
    IO.deleteRecursively(newTemp)
  }

  test("makeTemp with addRandomized disabled, folder doesn't exist") {
    val tempDir = IO.makeTemp("foo")
    val tempDirStr = tempDir.getFileName().toString()
    IO.deleteRecursively(tempDir)
    val newTemp = IO.makeTemp(tempDirStr, addRandomized = false)
    assert(Files.exists(newTemp) && Files.isDirectory(newTemp))

    assert(newTemp == tempDir)
    IO.deleteRecursively(newTemp)
  }

  test("makeTemp with addRandomized disabled, folder exists but not empty") {
    val tempDir = IO.makeTemp("foo")
    val tempDirStr = tempDir.getFileName().toString()
    IO.write("foo", tempDir.resolve("foo.txt"))

    val caught = intercept[RuntimeException] {
      IO.makeTemp(tempDirStr, addRandomized = false)
    }

    assert(caught.getMessage().contains(s"Temporary directory $tempDir already exists and is not empty."))

    IO.deleteRecursively(tempDir)
  }

  test("makeTemp with addRandomized disabled, folder exists as a file") {
    val tempDir = IO.makeTemp("foo")
    val tempDirStr = tempDir.getFileName().toString()
    IO.deleteRecursively(tempDir)
    IO.write("foo", tempDir)

    val caught = intercept[RuntimeException] {
      IO.makeTemp(tempDirStr, addRandomized = false)
    }

    assert(caught.getMessage().contains(s"Temporary directory $tempDir already exists as a file."))

    IO.deleteRecursively(tempDir)
  }

  test("uri with path") {
    val uri = IO.uri("test.txt")
    assert(uri.isInstanceOf[URI])
    assert(uri.getScheme() == "file")
    assert(uri.getHost() == null)
    assert(uri.getPath().endsWith("test.txt")) // path will be absolute
  }

  test("uri with url") {
    val uri = IO.uri("https://foo.com/bar")
    assert(uri.isInstanceOf[URI])
    assert(uri.getScheme() == "https")
    assert(uri.getHost() == "foo.com")
    assert(uri.getPath() == "/bar")
  }

  test("read and write") {
    val testPath = tempDir.resolve("test.txt")
    IO.write("Test content", testPath, overwrite = true)
    val content = IO.read(testPath.toUri)
    assert(content == "Test content")
  }

  test("readSome existing file") {
    val testPath = tempDir.resolve("test.txt")
    IO.write("Test content", testPath, overwrite = true)
    val content = IO.readSome(testPath.toUri)
    assert(content.isDefined && content.get == "Test content")
  }

  test("readSome nonexistent") {
    val testUri = tempDir.resolve("nonexistent.txt").toUri
    val content = IO.readSome(testUri)
    assert(content.isEmpty)
  }

  test("setPerms") {
    val testPath = tempDir.resolve("test.txt")
    val writtenPath = IO.write("Test content", testPath, overwrite = true)
    IO.setPerms(writtenPath, Some(true))
    val perms = Files.getPosixFilePermissions(writtenPath)
    assert(perms.contains(java.nio.file.attribute.PosixFilePermission.OWNER_EXECUTE))
  }

  test("resolvePathWrtURI") {
    val projectUri = tempDir.toUri()
    val result = IO.resolvePathWrtURI("/test/test.txt", projectUri)
    assert(result == "test/test.txt")
  }

  test("resolveProjectPath success") {
    val projectUri = tempDir.toUri()
    val expectedResult = tempDir.resolve("test/test.txt")
    val result = Try(IO.resolveProjectPath("/test/test.txt", Some(projectUri)))
    assert(result.isSuccess && result.get == expectedResult.toUri())
  }

  test("resolveProjectPath failure because prefix incorrect") {
    val projectUri = tempDir.toUri()
    val result = Try(IO.resolveProjectPath("test/test.txt", Some(projectUri)))
    assert(result.isFailure)
  }

  test("resolveProjectPath failure because project uri is none") {
    val result = Try(IO.resolveProjectPath("/test/test.txt", None))
    assert(result.isFailure)
  }
}
