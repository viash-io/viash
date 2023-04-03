package io.viash.helpers

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.BeforeAndAfter
import java.nio.file.{Files, Path}
import java.net.URI
import io.viash.helpers.IO
import scala.util.Try

class IOTest extends AnyFunSuite with BeforeAndAfter {
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

    IO.deleteRecursively(temp)
    assert(!Files.exists(temp))
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
