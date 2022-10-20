package io.viash.helpers

import org.scalatest.BeforeAndAfterAll
import org.scalatest.funsuite.AnyFunSuite

class EscaperTest extends AnyFunSuite with BeforeAndAfterAll {
    val s = "a \\ b $ c ` d \" e ' f \n g"
    
  test("escape with default parameters work") {
    assert(Escaper(s) == s)
  }
    
  test("escaping slash works") {
    assert(Escaper(s, slash = true) == "a \\\\ b $ c ` d \" e ' f \n g")
  }
    
  test("escaping dollar works") {
    assert(Escaper(s, dollar = true) == "a \\ b \\$ c ` d \" e ' f \n g")
  }
    
  test("escaping backtick works") {
    assert(Escaper(s, backtick = true) == "a \\ b $ c \\` d \" e ' f \n g")
  }
    
  test("escaping quote works") {
    assert(Escaper(s, quote = true) == "a \\ b $ c ` d \\\" e ' f \n g")
  }
    
  test("escaping singleQuote works") {
    assert(Escaper(s, singleQuote = true) == "a \\ b $ c ` d \" e \\' f \n g")
  }
    
  test("escaping newline works") {
    assert(Escaper(s, newline = true) == "a \\ b $ c ` d \" e ' f \\n g")
  }
}