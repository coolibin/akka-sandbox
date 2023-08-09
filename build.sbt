
lazy val org = "coolibin"
val appVersion = "0.1"
ThisBuild / scalaVersion := "2.12.16"
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

resolvers += "Akka library repository".at("https://repo.akka.io/maven")

//val AkkaVersion = "2.8.3"
val AkkaVersion = "2.6.14"


lazy val root = project.in(file("."))
  .settings(
    organization := org,
    version := appVersion,
    libraryDependencies += "com.typesafe.akka" %% "akka-actor-typed" % AkkaVersion,
    publishArtifact := true,
    Test / testOptions += Tests.Argument("-o")
  )
