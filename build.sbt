name := "viash"

version := "0.9.2"

scalaVersion := "3.3.4"

libraryDependencies ++= Seq(
  "org.scalactic" %% "scalactic" % "3.2.15" % "test",
  "org.scalatest" %% "scalatest" % "3.2.15" % "test",
  "org.rogach" %% "scallop" % "5.0.0",
  "org.scala-lang.modules" %% "scala-parser-combinators" % "2.1.1",
  "org.scala-lang.modules" %% "scala-parallel-collections" % "1.0.4",
  "dev.optics" %% "monocle-core"  % "3.1.0",
  "dev.optics" %% "monocle-macro" % "3.1.0"
)

val circeVersion = "0.14.7"

libraryDependencies ++= Seq(
  "io.circe" %% "circe-core",
  "io.circe" %% "circe-generic",
  "io.circe" %% "circe-parser",
  // "io.circe" %% "circe-generic-extras",
  // "io.circe" %% "circe-optics",
  // "io.circe" %% "circe-yaml"
).map(_ % circeVersion)

libraryDependencies ++= Seq(
  "io.circe" %% "circe-optics" % "0.15.0",
  "io.circe" %% "circe-yaml" % "0.15.2",
)

scalacOptions ++= Seq("-unchecked", "-deprecation", "-explain")
scalacOptions ++= Seq("-Xmax-inlines", "50")

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

  val rootDir = (Compile / baseDirectory).value
  val basePath = (Compile / resourceDirectory).value / "io" / "viash" / "runners" / "nextflow"
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
    val relativePath = rootDir.relativize(path)
    Files.write(wfHelper, s"\n// helper file: '${relativePath.get}'\n".getBytes(), StandardOpenOption.APPEND)
    Files.write(wfHelper, Files.readAllBytes(path.toPath()), StandardOpenOption.APPEND)
  }

  streams.value.log.info(s"Generated WorkflowHelper.nf at ${wfHelper.toAbsolutePath}")
}

assembly := ((assembly) dependsOn generateWorkflowHelper).value
Test / testOptions := ((Test / testOptions) dependsOn generateWorkflowHelper).value
