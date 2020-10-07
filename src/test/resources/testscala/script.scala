// helper functions

import java.io.FileWriter

val (logFun, logFile): (String => Unit, Option[FileWriter]) = par.log match {
  case Some(logPath) => 
    val lf = new FileWriter(logPath, true)
    ((s: String) => lf.write(s + "\n"), Some(lf))
  case None =>
    ((s: String) => println(s), None)
}
val (outputFun, outputFile): (String => Unit, Option[FileWriter]) = par.output match {
  case Some(outputPath) => 
    logFun("> Writing output to file")
    val lf = new FileWriter(outputPath, true)
    ((s: String) => lf.write(s + "\n"), Some(lf))
  case None =>
    logFun("> Printing output to console")
    ((s: String) => println(s), None)
}
def toStringWithFields(c: AnyRef): Map[String, Any] = {
  (Map[String, Any]() /: c.getClass.getDeclaredFields) { (a, f) =>
    f.setAccessible(true)
    a + (f.getName -> f.get(c))
  }
}

try {
  logFun("> Parsed input arguments.")

  val map = toStringWithFields(par)

  for ((name, value) ← map) {
    outputFun(s"$name: |$value|")
  }
  outputFun(s"resources_dir: |$resources_dir|")
  
} finally {
  logFile.map(_.close())
  outputFile.map(_.close())
}
