name := "viash"

version := "0.6.6"

scalaVersion := "2.12.15"

libraryDependencies ++= Seq(
  "org.scalactic" %% "scalactic" % "3.0.7" % "test",
  "org.scalatest" %% "scalatest" % "3.0.7" % "test",
  "org.rogach" %% "scallop" % "4.0.1",
  "org.scala-lang" % "scala-reflect" % scalaVersion.value,
  "org.scala-lang.modules" %% "scala-parser-combinators" % "1.1.2"
)

val circeVersion = "0.12.0"

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
