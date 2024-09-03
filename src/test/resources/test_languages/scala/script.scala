// helper functions

import java.io.FileWriter
import scala.io.Source

val (logFun, logFile): (String => Unit, Option[FileWriter]) = par.log match {
  case Some(logPath) => 
    val lf = new FileWriter(logPath, true)
    ((s: String) => lf.write("INFO:" + s + "\n"), Some(lf))
  case None =>
    ((s: String) => println(s), None)
}
val (outputFun, outputFile): (String => Unit, Option[FileWriter]) = par.output match {
  case Some(outputPath) => 
    logFun("Writing output to file")
    val lf = new FileWriter(outputPath, true)
    ((s: String) => lf.write(s + "\n"), Some(lf))
  case None =>
    logFun("Printing output to console")
    ((s: String) => println(s), None)
}
def toStringWithFields(c: AnyRef): Map[String, Any] = {
  c.getClass.getDeclaredFields.foldLeft(Map.empty[String, Any]) { (a, f) =>
    f.setAccessible(true)
    a + (f.getName -> f.get(c))
  }
}

try {
  logFun("Parsed input arguments.")

  for ((name, value) <- toStringWithFields(par)) {
    value match {
      case Some(some) => outputFun(s"$name: |$some|")
      case None => outputFun(s"$name: ||")
      case list: List[_] => outputFun(s"""$name: |${list.mkString(";")}|""")
      case _ => outputFun(s"$name: |$value|")
    }
  }

  for ((name, value) <- toStringWithFields(meta)) {
    value match {
      case Some(some) => outputFun(s"meta_$name: |$some|")
      case None => outputFun(s"meta_$name: ||")
      case list: List[_] => outputFun(s"""meta_$name: |${list.mkString(";")}|""")
      case _ => outputFun(s"meta_$name: |$value|")
    }
  }

  val input = Source.fromFile(par.input).getLines().toArray
  outputFun(s"head of input: |${input(0)}|")
  val resource1 = Source.fromFile("resource1.txt").getLines().toArray
  outputFun(s"head of resource1: |${resource1(0)}|")
  
} finally {
  logFile.map(_.close())
  outputFile.map(_.close())
}
