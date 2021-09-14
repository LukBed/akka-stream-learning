name := "akka-stream-learning"

version := "0.1"

scalaVersion := "2.13.6"

lazy val akkaVersion = "2.6.16"
lazy val scalaTestVersion = "3.2.3"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion % Test,
  "com.typesafe.akka" %% "akka-stream-testkit" % akkaVersion % Test,
  "org.scalatest" %% "scalatest" % scalaTestVersion % Test
)
