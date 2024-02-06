package io.viash.helpers

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.BeforeAndAfter
import java.nio.file.{Files, Path}
import java.net.URI
import io.viash.helpers.{IO, Logger}
import scala.util.Try
import java.nio.file.Paths

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
    val packageUri = tempDir.toUri()
    val result = IO.resolvePathWrtURI("/test/test.txt", packageUri)
    assert(result == "test/test.txt")
  }

  test("resolvePackagePath success") {
    val packageUri = tempDir.toUri()
    val expectedResult = tempDir.resolve("test/test.txt")
    val result = Try(IO.resolvePackagePath("/test/test.txt", Some(packageUri)))
    assert(result.isSuccess && result.get == expectedResult.toUri())
  }

  test("resolvePackagePath failure because prefix incorrect") {
    val packageUri = tempDir.toUri()
    val result = Try(IO.resolvePackagePath("test/test.txt", Some(packageUri)))
    assert(result.isFailure)
  }

  test("resolvePackagePath failure because package uri is none") {
    val result = Try(IO.resolvePackagePath("/test/test.txt", None))
    assert(result.isFailure)
  }

  test("anonymizePath with a common path") {
    val basePath = Some(Paths.get("/foo/bar"))
    val path = "/foo/bar/baz/test.txt"
    val result = IO.anonymizePath(basePath, path)
    assert(result == "baz/test.txt")
  }

  test("anonymizePath with a different base path") {
    val basePath = Some(Paths.get("/foo/bar"))
    val path = "/foo/baz/test.txt"
    val result = IO.anonymizePath(basePath, path)
    assert(result == "[anonymized]/test.txt")
  }

  test("anynimizePath with a completely different base path") {
    val basePath = Some(Paths.get("/foo/bar"))
    val path = "/tmp/foo/bar/baz/test.txt"
    val result = IO.anonymizePath(basePath, path)
    assert(result == "[anonymized]/test.txt")
  }

  test("anonymizePath with a common path with a trailing slash") {
    val basePath = Some(Paths.get("/foo/bar/"))
    val path = "/foo/bar/baz/test.txt"
    val result = IO.anonymizePath(basePath, path)
    assert(result == "baz/test.txt")
  }

  test("anonymizePath without a base path") {
    val basePath = None
    val path = "/foo/bar/baz/test.txt"
    val result = IO.anonymizePath(basePath, path)
    assert(result == "[anonymized]/test.txt")
  }
}
