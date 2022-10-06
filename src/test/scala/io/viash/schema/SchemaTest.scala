package io.viash.schema

import org.scalatest.{BeforeAndAfterAll, FunSuite}
import io.viash.schemas.CollectedSchemas

class SchemaTest extends FunSuite with BeforeAndAfterAll {
    
  test("All schema class val members should be annotated") {
    val nonAnnotated = CollectedSchemas.getAllNonAnnotated
    nonAnnotated.foreach {
     case (key, key2, member) => Console.err.println(s"$key - $key2 - $member")
    }
    assert(nonAnnotated.size == 0)
  }
    
}