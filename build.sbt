
lazy val org = "coolibin"
val Scala = "2.12.16"
val AppVersion = "0.1"
val AkkaVersion = "2.6.14"
val ScalaTest = "3.2.3"
val LogBack = "1.1.7"

lazy val root = project.in(file("."))
  .settings(
    name := "akka-sandbox",
    organization := org,
    version := AppVersion,
    resolvers ++= Seq(
      "Akka library repository".at("https://repo.akka.io/maven"),
      DefaultMavenRepository
    ),

    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-actor-typed" % AkkaVersion,
      "ch.qos.logback" % "logback-classic" % LogBack,
      "org.scalatest" %% "scalatest" % ScalaTest
    ),
    publishArtifact := true,
    Test / testOptions += Tests.Argument("-o")
  )

ThisBuild / scalaVersion := Scala
ThisBuild / scalacOptions ++= Seq(
  "-feature",
  "-unchecked",
  "-language:postfixOps",
  "-deprecation:false",
  "-explaintypes",
  "-language:implicitConversions",
  "-language:reflectiveCalls",
  "-language:existentials"
)