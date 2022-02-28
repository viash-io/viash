name := "viash"

version := "0.5.8"

scalaVersion := "2.12.10"

libraryDependencies ++= Seq(
  "org.scalactic" %% "scalactic" % "3.0.7" % "test",
  "org.scalatest" %% "scalatest" % "3.0.7" % "test",
  "org.rogach" %% "scallop" % "4.0.1",
  "com.github.eikek" %% "yamusca-core" % "0.5.1",
  "io.circe" %% "circe-yaml" % "0.12.0",
  "org.scala-lang" % "scala-reflect" % scalaVersion.value,
  "org.scala-lang.modules" %% "scala-parser-combinators" % "1.1.2"
)

val circeVersion = "0.12.0"

libraryDependencies ++= Seq(
  "io.circe" %% "circe-core",
  "io.circe" %% "circe-generic",
  "io.circe" %% "circe-parser",
  "io.circe" %% "circe-generic-extras",
  "io.circe" %% "circe-optics"
).map(_ % circeVersion)

scalacOptions ++= Seq("-unchecked", "-deprecation")

organization := "Data Intuitive"
startYear := Some(2020)
licenses += "GPL-3.0-or-later" -> url("https://www.gnu.org/licenses/gpl-3.0.html")

Test / parallelExecution := false
