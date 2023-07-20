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
        logger.infoOut("bar")
      }
    }

    val stdout = outStream.toString
    val stderr = errStream.toString

    assert(stdout == s"${AnsiColor.WHITE}bar${AnsiColor.RESET}\n")
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
        logger.infoOut("bar")
      }
    }

    val stdout = outStream.toString
    val stderr = errStream.toString

    assert(stdout == "bar\n")
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

  test("Check is*Enabled methods") {
    val logger = Logger.apply("Tester")

    assert(logger.isErrorEnabled == true)
    assert(logger.isWarnEnabled == true)
    assert(logger.isInfoEnabled == true)
    assert(logger.isDebugEnabled == false)
    assert(logger.isTraceEnabled == false)
  }

  test("Check LoggerLevel from string") {
    assert(LoggerLevel.fromString("error") == LoggerLevel.Error)
    assert(LoggerLevel.fromString("warn") == LoggerLevel.Warn)
    assert(LoggerLevel.fromString("info") == LoggerLevel.Info)
    assert(LoggerLevel.fromString("debug") == LoggerLevel.Debug)
    assert(LoggerLevel.fromString("trace") == LoggerLevel.Trace)
    assertThrows[RuntimeException](LoggerLevel.fromString("foo"))
  }

  test("Check all level prints") {
    val logger = Logger.apply("Tester_prints_color", LoggerLevel.Trace, true)

    val outStream = new ByteArrayOutputStream()
    val errStream = new ByteArrayOutputStream()
    Console.withOut(outStream) {
      Console.withErr(errStream) {
        logger.error("err: error")
        logger.warn("err: warn")
        logger.info("err: info")
        logger.debug("err: debug")
        logger.trace("err: trace")
        logger.success("err: success")

        logger.errorOut("out: error")
        logger.warnOut("out: warn")
        logger.infoOut("out: info")
        logger.debugOut("out: debug")
        logger.traceOut("out: trace")
        logger.successOut("out: success")

        logger.log(LoggerOutput.StdErr, LoggerLevel.Error, AnsiColor.MAGENTA, "err: foo")
        logger.log(LoggerOutput.StdOut, LoggerLevel.Error, AnsiColor.BLUE, "out: foo")
      }
    }

    val stdout = outStream.toString
    val stderr = errStream.toString

    val expectOut =
      s"""${AnsiColor.RED}out: error${AnsiColor.RESET}
         |${AnsiColor.YELLOW}out: warn${AnsiColor.RESET}
         |${AnsiColor.WHITE}out: info${AnsiColor.RESET}
         |${AnsiColor.GREEN}out: debug${AnsiColor.RESET}
         |${AnsiColor.CYAN}out: trace${AnsiColor.RESET}
         |${AnsiColor.GREEN}out: success${AnsiColor.RESET}
         |${AnsiColor.BLUE}out: foo${AnsiColor.RESET}
         |""".stripMargin

    val expectErr =
      s"""${AnsiColor.RED}err: error${AnsiColor.RESET}
         |${AnsiColor.YELLOW}err: warn${AnsiColor.RESET}
         |${AnsiColor.WHITE}err: info${AnsiColor.RESET}
         |${AnsiColor.GREEN}err: debug${AnsiColor.RESET}
         |${AnsiColor.CYAN}err: trace${AnsiColor.RESET}
         |${AnsiColor.GREEN}err: success${AnsiColor.RESET}
         |${AnsiColor.MAGENTA}err: foo${AnsiColor.RESET}
         |""".stripMargin

    assert(stdout == expectOut)
    assert(stderr == expectErr)
  }

}
