name := "viash"

version := "0.3.1rc"

scalaVersion := "2.12.10"

libraryDependencies ++= Seq(
  "org.scalactic" %% "scalactic" % "3.0.7" % "test",
  "org.scalatest" %% "scalatest" % "3.0.7" % "test",
  "org.rogach" %% "scallop" % "4.0.1",
  "com.github.eikek" %% "yamusca-core" % "0.5.1",
  "io.circe" %% "circe-yaml" % "0.12.0",
  "org.scala-lang" % "scala-reflect" % scalaVersion.value
)

val circeVersion = "0.12.0"

libraryDependencies ++= Seq(
  "io.circe" %% "circe-core",
  "io.circe" %% "circe-generic",
  "io.circe" %% "circe-parser",
  "io.circe" %% "circe-generic-extras"
).map(_ % circeVersion)

scalacOptions ++= Seq("-unchecked", "-deprecation")

organization := "Data Intuitive"

licenses += ("Custom", url("file://LICENSE.md"))
