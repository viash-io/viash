/*
 * Copyright (C) 2020  Data Intuitive
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.viash.helpers

import scala.io.AnsiColor

object LoggerLevel extends Enumeration {
  type Level = Value
  val Error = Value(3)
  val Warn = Value(4)
  val Info = Value(6)
  val Debug = Value(7)
  val Trace = Value(8)
  
  val Success = Value(5) // Should have been 6, same as Info. Luckely we had a spare spot between 4 and 6

  def fromString(level: String): Value = {
    level match {
      case "error" => Error
      case "warn" => Warn
      case "info" => Info
      case "debug" => Debug
      case "trace" => Trace
      case _ => throw new IllegalArgumentException(level)
    }
  }
}

object LoggerOutput extends Enumeration {
  type Output = Value
  val StdOut = Value(1)
  val StdErr = Value(2)
}

/** Partial logging facade styled alike SLF4J.
  * Used Grizzled slf4j as further inspiration and basis.
  */
class Logger(val name: String, val level: LoggerLevel.Level, val useColor: Boolean) {
  import LoggerOutput._
  import LoggerLevel._

  @inline final def isErrorEnabled = isEnabled(Error)
  @inline final def isWarnEnabled = isEnabled(Warn)
  @inline final def isInfoEnabled = isEnabled(Info)
  @inline final def isDebugEnabled = isEnabled(Debug)
  @inline final def isTraceEnabled = isEnabled(Trace)

  @inline final def error(msg: => Any): Unit = _log(Error, msg)
  @inline final def warn(msg: => Any): Unit = _log(Warn, msg)
  @inline final def info(msg: => Any): Unit = _log(Info, msg)
  @inline final def debug(msg: => Any): Unit = _log(Debug, msg)
  @inline final def trace(msg: => Any): Unit = _log(Trace, msg)
  @inline final def success(msg: => Any): Unit = _log(Success, msg)

  @inline final def errorOut(msg: => Any): Unit = _logOut(Error, msg)
  @inline final def warnOut(msg: => Any): Unit = _logOut(Warn, msg)
  @inline final def infoOut(msg: => Any): Unit = _logOut(Info, msg)
  @inline final def debugOut(msg: => Any): Unit = _logOut(Debug, msg)
  @inline final def traceOut(msg: => Any): Unit = _logOut(Trace, msg)
  @inline final def successOut(msg: => Any): Unit = _logOut(Success, msg)

  @inline final def isEnabled(level: Level): Boolean = this.level >= level

  @inline private def _colorString(level: Level): String =
    level match {
      case Error => AnsiColor.RED
      case Warn => AnsiColor.YELLOW
      case Info => AnsiColor.WHITE
      case Debug => AnsiColor.GREEN
      case Trace => AnsiColor.CYAN

      case Success => AnsiColor.GREEN
    }

  @inline private def _log(level: Level, msg: => Any): Unit = {
    if (!isEnabled(level)) return
    
    if (useColor)
      Console.err.println(s"${_colorString(level)}${msg.toString()}${AnsiColor.RESET}")
    else
      Console.err.println(msg.toString())
  }

  @inline private def _logOut(level: Level, msg: => Any): Unit = {
    if (!isEnabled(level)) return
    
    if (useColor)
      Console.out.println(s"${_colorString(level)}${msg.toString()}${AnsiColor.RESET}")
    else
      Console.out.println(msg.toString())
  }

  @inline def log(out: Output, level: Level, color: String, msg: => Any): Unit = {
    if (!isEnabled(level)) return

    val printer = 
      if (out == LoggerOutput.StdErr)
        Console.err
      else
        Console.out
    
    if (useColor)
      printer.println(s"${color}${msg.toString()}${AnsiColor.RESET}")
    else
      printer.println(msg.toString())
  }
}

trait Logging {
  // The logger. Instantiated the first time it's used.
  @transient private lazy val _logger = Logger(getClass)

  protected def logger: Logger = _logger
  protected def loggerName = logger.name

  protected def isErrorEnabled = logger.isErrorEnabled
  protected def isWarnEnabled = logger.isWarnEnabled
  protected def isInfoEnabled = logger.isInfoEnabled
  protected def isDebugEnabled = logger.isDebugEnabled
  protected def isTraceEnabled = logger.isTraceEnabled

  protected def error(msg: => Any): Unit = logger.error(msg)
  protected def warn(msg: => Any): Unit = logger.warn(msg)
  protected def info(msg: => Any): Unit = logger.info(msg)
  protected def debug(msg: => Any): Unit = logger.debug(msg)
  protected def trace(msg: => Any): Unit = logger.trace(msg)
  protected def success(msg: => Any): Unit = logger.success(msg)

  protected def errorOut(msg: => Any): Unit = logger.errorOut(msg)
  protected def warnOut(msg: => Any): Unit = logger.warnOut(msg)
  protected def infoOut(msg: => Any): Unit = logger.infoOut(msg)
  protected def debugOut(msg: => Any): Unit = logger.debugOut(msg)
  protected def traceOut(msg: => Any): Unit = logger.traceOut(msg)
  protected def successOut(msg: => Any): Unit = logger.successOut(msg)

  protected def log(out: LoggerOutput.Output, level: LoggerLevel.Level, color: String, msg: => Any): Unit = logger.log(out, level, color, msg)
}

object Logger {
  import scala.reflect.{classTag, ClassTag}

  val rootLoggerName = "Viash-root-logger"

  def apply(name: String, level: LoggerLevel.Level, useColor: Boolean): Logger = new Logger(name, level, useColor)
  def apply(name: String): Logger = new Logger(name, getLoggerLevel(name), useColor)
  def apply(cls: Class[_]): Logger = apply(cls.getName)
  def apply[C: ClassTag](): Logger = apply(classTag[C].runtimeClass.getName)

  def rootLogger = apply(rootLoggerName)

  object UseLevelOverride extends util.DynamicVariable[LoggerLevel.Level](LoggerLevel.Info)
  def getLoggerLevel(name: String): LoggerLevel.Level = {
    if (name != rootLoggerName) // prevent constructor loop
      rootLogger.debug(s"GetLoggerLevel for $name")

    if (name == "io.viash.helpers.LoggerTest$ClassTraitLoggingTest$1")
      return LoggerLevel.Trace

    // TODO setting of logger levels for individual loggers
    UseLevelOverride.value
  }

  object UseColorOverride extends util.DynamicVariable[Option[Boolean]](None)
  private val useColor_ = System.console() != null
  def useColor = UseColorOverride.value.getOrElse(useColor_)
}
