package io.viash.helpers

import org.scalatest.funsuite.AnyFunSuite
import java.io.ByteArrayOutputStream
import scala.io.AnsiColor
import io.viash.TestHelper
import java.io.FileNotFoundException

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

  test("Check logger as a class trait") {

    class ClassTraitLoggingTest extends Logging {
      def bar(): Unit = {
        error(s"err: error $isErrorEnabled")
        warn(s"err: warn $isWarnEnabled")
        info(s"err: info $isInfoEnabled")
        debug(s"err: debug $isDebugEnabled")
        trace(s"err: trace $isTraceEnabled")
        success(s"err: success")

        errorOut(s"out: error")
        warnOut(s"out: warn")
        infoOut(s"out: info")
        debugOut(s"out: debug")
        traceOut(s"out: trace")
        successOut(s"out: success")

        log(LoggerOutput.StdErr, LoggerLevel.Error, AnsiColor.MAGENTA, "err: foo")
        log(LoggerOutput.StdOut, LoggerLevel.Error, AnsiColor.BLUE, "out: foo")
      }

      def name(): String = loggerName
      def level(): String = logger.level.toString()
    }

    val testClass = new ClassTraitLoggingTest()

    assert(testClass.name() == "io.viash.helpers.LoggerTest$ClassTraitLoggingTest$1")
    assert(testClass.level() == "Trace")

    val outStream = new ByteArrayOutputStream()
    val errStream = new ByteArrayOutputStream()
    Console.withOut(outStream) {
      Console.withErr(errStream) {
        testClass.bar()
      }
    }

    val stdout = outStream.toString
    val stderr = errStream.toString

    val expectOut =
      s"""out: error
         |out: warn
         |out: info
         |out: debug
         |out: trace
         |out: success
         |out: foo
         |""".stripMargin

    val expectErr =
      s"""err: error true
         |err: warn true
         |err: info true
         |err: debug true
         |err: trace true
         |err: success
         |err: foo
         |""".stripMargin

    assert(stdout == expectOut)
    assert(stderr == expectErr)

    // Tack on tests for a variant class
    class ClassTraitLoggingTest2 extends ClassTraitLoggingTest
    val testClass2 = new ClassTraitLoggingTest2()

    assert(testClass2.name() == "io.viash.helpers.LoggerTest$ClassTraitLoggingTest2$1")
    assert(testClass2.level() == "Info")

    val outStream2 = new ByteArrayOutputStream()
    val errStream2 = new ByteArrayOutputStream()
    Console.withOut(outStream2) {
      Console.withErr(errStream2) {
        testClass2.bar()
      }
    }

    val stdout2 = outStream2.toString
    val stderr2 = errStream2.toString

    val expectOut2 =
      s"""out: error
         |out: warn
         |out: info
         |out: success
         |out: foo
         |""".stripMargin

    val expectErr2 =
      s"""err: error true
         |err: warn true
         |err: info true
         |err: success
         |err: foo
         |""".stripMargin

    assert(stdout2 == expectOut2)
    assert(stderr2 == expectErr2)
  }

  // We can't really test the colorize or loglevel options as the singletons would need to be recreated.
  // However we can verify that the parsing happened correctly and set the inner logger values correctly.

  test("Check without --colorize option") {
    Logger.UseColorOverride.value = Some(false)
    TestHelper.testMainException[FileNotFoundException](
      "config", "view", "missing.vsh.yaml",
    )
    assert(Logger.UseColorOverride.value == Some(false))

    Logger.UseColorOverride.value = Some(true)
    TestHelper.testMainException[FileNotFoundException](
      "config", "view", "missing.vsh.yaml",
    )
    assert(Logger.UseColorOverride.value == Some(true))

    Logger.UseColorOverride.value = Some(false)
  }

  test("Check --colorize true option") {
    TestHelper.testMainException[FileNotFoundException](
      "config", "view", "missing.vsh.yaml",
      "--colorize", "true"
    )
    assert(Logger.UseColorOverride.value == Some(true))
    Logger.UseColorOverride.value = Some(false)
  }

  test("Check --colorize false option") {
    TestHelper.testMainException[FileNotFoundException](
      "config", "view", "missing.vsh.yaml",
      "--colorize", "false"
    )
    assert(Logger.UseColorOverride.value == Some(false))
    Logger.UseColorOverride.value = Some(false)
  }

  test("Check --colorize auto option") {
    TestHelper.testMainException[FileNotFoundException](
      "config", "view", "missing.vsh.yaml",
      "--colorize", "auto"
    )
    assert(Logger.UseColorOverride.value == None)
    Logger.UseColorOverride.value = Some(false)
  }

  test("Check --loglevel debug") {
    assert(Logger.UseLevelOverride.value == LoggerLevel.Info)
    TestHelper.testMainException[FileNotFoundException](
      "config", "view", "missing.vsh.yaml",
      "--loglevel", "debug"
    )
    assert(Logger.UseLevelOverride.value == LoggerLevel.Debug)
    Logger.UseLevelOverride.value = LoggerLevel.Info
  }

  test("Check without --loglevel set") {
    assert(Logger.UseLevelOverride.value == LoggerLevel.Info)
    TestHelper.testMainException[FileNotFoundException](
      "config", "view", "missing.vsh.yaml",
    )
    assert(Logger.UseLevelOverride.value == LoggerLevel.Info)
    Logger.UseLevelOverride.value = LoggerLevel.Info
  }

}
