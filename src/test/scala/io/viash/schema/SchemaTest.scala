package io.viash.schema

import org.scalatest.BeforeAndAfterAll
import org.scalatest.funsuite.AnyFunSuite
import io.viash.schemas.CollectedSchemas
import scala.sys.process.Process

class SchemaTest extends AnyFunSuite with BeforeAndAfterAll {
    
  test("All schema class val members should be annotated") {
    val nonAnnotated = CollectedSchemas.getAllNonAnnotated
    nonAnnotated.foreach {
     case (key, member) => Console.err.println(s"$key - $member")
    }
    assert(nonAnnotated.size == 0)
  }

  test("Check formatting of deprecation annotations") {
    val data = CollectedSchemas.getAllDeprecations

    for ((name, depAnn) <- data) {
      val regex = """^\d+\.\d+\.\d+$"""
      assert(depAnn.deprecation.matches(regex), s"$name deprecation version ${depAnn.deprecation} doesn't match $regex")
      assert(depAnn.removal.matches(regex), s"$name planned removal version ${depAnn.removal} doesn't match $regex")
    }
  }

  test("Check deprecated arguments are due for removal") {

    def compareVersions(version1: String, version2: String) = 
       version1.split("\\.")
        .zipAll(version2.split("\\."), "0", "0")
        .find { case(a, b) => a != b }
        .fold(0) { case (a, b) => a.toInt - b.toInt }

    val data = CollectedSchemas.getAllDeprecations
    
    // Manually get the version number from build.sbt as this information is not present during tests
    val currentVersion = Process(Seq("awk", "-F\"", "/version :=/ { print $2 }", "build.sbt")).!!.trim()

    for ((name, depAnn) <- data) {
      val res = compareVersions(depAnn.removal, currentVersion)

      assert(res > 0, s"Name: $name , Planned removal: ${depAnn.removal} , Current version: $currentVersion , Compare result: $res")
    }
  }
    
}
