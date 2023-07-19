package io.viash.helpers

import org.scalatest.funsuite.AnyFunSuite
import java.io.ByteArrayOutputStream
import scala.io.AnsiColor

class LoggerTest extends AnyFunSuite {
  Logger.UseColorOverride.value = Some(false)

  test("Check basic log print") {
    val logger = Logger.apply("Tester")

    assert(logger.name == "Tester")
    assert(logger.useColor == false)

    val outStream = new ByteArrayOutputStream()
    val errStream = new ByteArrayOutputStream()
    Console.withOut(outStream) {
      Console.withErr(errStream) {
        logger.info("foo")
      }
    }

    val stdout = outStream.toString
    val stderr = errStream.toString

    assert(stdout.isEmpty())
    assert(stderr == "foo\n")
  }

  test("Check print to stdout") {
    val logger = Logger.apply("Tester")

    assert(logger.name == "Tester")
    assert(logger.useColor == false)

    val outStream = new ByteArrayOutputStream()
    val errStream = new ByteArrayOutputStream()
    Console.withOut(outStream) {
      Console.withErr(errStream) {
        logger.infoOut("foo")
      }
    }

    val stdout = outStream.toString
    val stderr = errStream.toString

    assert(stdout == "foo\n")
    assert(stderr.isEmpty())
  }

  test("Check printing in color") {
    val logger = Logger.apply("Tester_color", LoggerLevel.Info, true)

    assert(logger.name == "Tester_color")
    assert(logger.useColor == true)

    val outStream = new ByteArrayOutputStream()
    val errStream = new ByteArrayOutputStream()
    Console.withOut(outStream) {
      Console.withErr(errStream) {
        logger.info("foo")
      }
    }

    val stdout = outStream.toString
    val stderr = errStream.toString

    assert(stdout.isEmpty())
    assert(stderr == s"${AnsiColor.WHITE}foo${AnsiColor.RESET}\n")
  }

  test("Check printing without color") {
    val logger = Logger.apply("Tester_no_color", LoggerLevel.Info, false)

    assert(logger.name == "Tester_no_color")
    assert(logger.useColor == false)

    val outStream = new ByteArrayOutputStream()
    val errStream = new ByteArrayOutputStream()
    Console.withOut(outStream) {
      Console.withErr(errStream) {
        logger.info("foo")
      }
    }

    val stdout = outStream.toString
    val stderr = errStream.toString

    assert(stdout.isEmpty())
    assert(stderr == "foo\n")
  }

  test("Check error log print") {
    val logger = Logger.apply("Tester_error")

    assert(logger.name == "Tester_error")
    assert(logger.useColor == false)

    val outStream = new ByteArrayOutputStream()
    val errStream = new ByteArrayOutputStream()
    Console.withOut(outStream) {
      Console.withErr(errStream) {
        logger.error("foo")
      }
    }

    val stdout = outStream.toString
    val stderr = errStream.toString

    assert(stdout.isEmpty())
    assert(stderr == "foo\n")
  }

  test("Check warn log print") {
    val logger = Logger.apply("Tester_warn")

    assert(logger.name == "Tester_warn")
    assert(logger.useColor == false)

    val outStream = new ByteArrayOutputStream()
    val errStream = new ByteArrayOutputStream()
    Console.withOut(outStream) {
      Console.withErr(errStream) {
        logger.warn("foo")
      }
    }

    val stdout = outStream.toString
    val stderr = errStream.toString

    assert(stdout.isEmpty())
    assert(stderr == "foo\n")
  }

  test("Check debug log print while minimum level is info") {
    val logger = Logger.apply("Tester_debug")

    assert(logger.name == "Tester_debug")
    assert(logger.useColor == false)

    val outStream = new ByteArrayOutputStream()
    val errStream = new ByteArrayOutputStream()
    Console.withOut(outStream) {
      Console.withErr(errStream) {
        logger.debug("foo")
      }
    }

    val stdout = outStream.toString
    val stderr = errStream.toString

    assert(stdout.isEmpty())
    assert(stderr.isEmpty())
  }

  test("Check debug log print while minimum level is debug") {
    val logger = Logger.apply("Tester_debug2", LoggerLevel.Debug, false)

    assert(logger.name == "Tester_debug2")
    assert(logger.useColor == false)

    val outStream = new ByteArrayOutputStream()
    val errStream = new ByteArrayOutputStream()
    Console.withOut(outStream) {
      Console.withErr(errStream) {
        logger.debug("foo")
      }
    }

    val stdout = outStream.toString
    val stderr = errStream.toString

    assert(stdout.isEmpty())
    assert(stderr == "foo\n")
  }

}
