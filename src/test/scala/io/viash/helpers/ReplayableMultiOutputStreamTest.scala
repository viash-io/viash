package io.viash.helpers

import org.scalatest.funsuite.AnyFunSuite
import java.io.ByteArrayOutputStream



class ReplayableMultiOutputStreamTest extends AnyFunSuite {
  Logger.UseColorOverride.value = Some(false)

  def foo(logger: Logger): Unit = {
    logger.info("test1")
    logger.infoOut("test2")
    logger.info("test3")
    logger.infoOut("test4")
    logger.info("test5")
  }

  test("Check logging using stdout and stderr, text order is mixed") {
    val logger = Logger.apply("Tester1")

    val outStream = new ByteArrayOutputStream()
    val errStream = new ByteArrayOutputStream()
    Console.withOut(outStream) {
      Console.withErr(errStream) {
        foo(logger)
      }
    }

    val combinedOutput = outStream.toString + errStream.toString

    assert(combinedOutput == "test2\ntest4\ntest1\ntest3\ntest5\n")
  }

  test("Check logging with ReplayableMultiOutputStream, text order is preserved") {
    val logger = Logger.apply("Tester2")

    // The testing is quite involved as we need another stream to capture the output while normally we'll just print it to the console
    val outStream = new ByteArrayOutputStream()

    val multiStream = new ReplayableMultiOutputStream()
    Console.withOut(multiStream.getOutputStream(s => outStream.write(s.getBytes()))) {
      Console.withErr(multiStream.getOutputStream(s => outStream.write(s.getBytes()))) {
        foo(logger)
      }
    }

    multiStream.replay()

    val combinedOutput = outStream.toString

    assert(combinedOutput == "test1\ntest2\ntest3\ntest4\ntest5\n")
  }

  test("Check logging with ReplayableMultiOutputStream, text order is preserved. Try helper method to output as a single string.") {
    val logger = Logger.apply("Tester3")

    val multiStream = new ReplayableMultiOutputStream()
    Console.withOut(multiStream.getOutputStream(_ => ())) {
      Console.withErr(multiStream.getOutputStream(_ => ())) {
        foo(logger)
      }
    }

    val combinedOutput = multiStream.toString()

    assert(combinedOutput == "test1\ntest2\ntest3\ntest4\ntest5\n")
  }

}
