name := "viash"

version := "0.2.0.1"

scalaVersion := "2.12.10"

libraryDependencies ++= Seq(
  "org.scalactic" %% "scalactic" % "3.0.7" % "test",
  "org.scalatest" %% "scalatest" % "3.0.7" % "test",
  "org.rogach" %% "scallop" % "3.3.2",
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

organization := "com.data-intuitive"

licenses += ("Apache-2.0", url("https://www.apache.org/licenses/LICENSE-2.0.html"))
