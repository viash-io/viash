name := "viash"

version := "0.8.0dev"

scalaVersion := "2.13.10"

libraryDependencies ++= Seq(
  "org.scalactic" %% "scalactic" % "3.2.15" % "test",
  "org.scalatest" %% "scalatest" % "3.2.15" % "test",
  "org.rogach" %% "scallop" % "5.0.0",
  "org.scala-lang" % "scala-reflect" % scalaVersion.value,
  "org.scala-lang.modules" %% "scala-parser-combinators" % "2.1.1",
  "org.scala-lang.modules" %% "scala-parallel-collections" % "1.0.4",
  "com.github.julien-truffaut" %% "monocle-core"  % "2.1.0",
  "com.github.julien-truffaut" %% "monocle-macro" % "2.1.0"
)

val circeVersion = "0.14.1"

libraryDependencies ++= Seq(
  "io.circe" %% "circe-core",
  "io.circe" %% "circe-generic",
  "io.circe" %% "circe-parser",
  "io.circe" %% "circe-generic-extras",
  "io.circe" %% "circe-optics",
  "io.circe" %% "circe-yaml"
).map(_ % circeVersion)

scalacOptions ++= Seq("-unchecked", "-deprecation")

organization := "Data Intuitive"
startYear := Some(2020)
licenses += "GPL-3.0-or-later" -> url("https://www.gnu.org/licenses/gpl-3.0.html")

// tried adding viash components to path for testing
// unmanagedClasspath in Test += baseDirectory.value / "src" / "viash"


// Test / parallelExecution := false

lazy val generateWorkflowHelper = taskKey[Unit]("Generates WorkflowHelper.nf")

generateWorkflowHelper := {
  import sbt._
  import java.nio.file._

  val basePath = (Compile / resourceDirectory).value / "io" / "viash" / "platforms" / "nextflow"
  val wfHelper = Paths.get(basePath.toString, "WorkflowHelper.nf")

  // brute force recursive file listing instead of using a glob that skips files in the root directory
  def recursiveListFiles(f: File): Array[File] = {
    val these = f.listFiles
    these.filter(_.isFile()) ++ these.filter(_.isDirectory).flatMap(recursiveListFiles)
  }
  // skip files in the root directory
  val files = basePath.listFiles().filter(p => p.isDirectory()).map(f => recursiveListFiles(f)).flatten
  
  Files.deleteIfExists(wfHelper)
  Files.write(wfHelper, 
    """////////////////////////////
      |// VDSL3 helper functions //
      |////////////////////////////
      |""".stripMargin.getBytes(),
    StandardOpenOption.CREATE)

  files.sorted.foreach { path =>
    Files.write(wfHelper, s"\n// helper file: '${path}'\n".getBytes(), StandardOpenOption.APPEND)
    Files.write(wfHelper, Files.readAllBytes(path.toPath()), StandardOpenOption.APPEND)
  }

  streams.value.log.info(s"Generated WorkflowHelper.nf at ${wfHelper.toAbsolutePath}")
}

assembly := ((assembly) dependsOn generateWorkflowHelper).value
Test / testOptions := ((Test / testOptions) dependsOn generateWorkflowHelper).value