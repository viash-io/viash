package io.viash.helpers

import org.scalatest.funsuite.AnyFunSuite
import java.io.ByteArrayOutputStream
import scala.io.AnsiColor

class LoggerTest extends AnyFunSuite {

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
    Logger.UseColorOverride.value = Some(true)
    val logger = Logger.apply("Tester_color")

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
    Logger.UseColorOverride.value = Some(false)
    val logger = Logger.apply("Tester_no_color")

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

  test("Check printing auto color") {
    Logger.UseColorOverride.value = Some(false)
    val logger = Logger.apply("Tester_auto_color")

    assert(logger.name == "Tester_auto_color")
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
    Logger.UseLevelOverride.value = LoggerLevel.Debug
    val logger = Logger.apply("Tester_debug2")

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
