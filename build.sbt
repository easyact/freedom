import sbtassembly.Log4j2MergeStrategy
import sbtrelease.Version

name := "freedom"

resolvers += Resolver.sonatypeRepo("public")
scalaVersion := "2.12.10"
releaseNextVersion := { ver =>
  Version(ver).map(_.bumpMinor.string).getOrElse("Error")
}
assemblyJarName in assembly := "hello.jar"

libraryDependencies ++= Seq(
  "com.amazonaws" % "aws-lambda-java-events" % "2.2.7",
  "com.amazonaws" % "aws-lambda-java-core" % "1.2.0",
  "com.amazonaws" % "aws-lambda-java-log4j2" % "1.1.0",
  "com.amazonaws" % "aws-java-sdk-dynamodb" % "1.11.716",
  "org.scalaz" %% "scalaz-concurrent" % "7.3.0-M27",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.9.2",
  "com.fasterxml.jackson.core" % "jackson-databind" % "2.10.1",
  "com.fasterxml.jackson.datatype" % "jackson-datatype-jsr310" % "2.10.1",
  "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.10.2"
)

libraryDependencies += "io.cucumber" % "cucumber-java" % "5.0.0-RC4" % Test
libraryDependencies += "org.assertj" % "assertj-core" % "3.14.0" % Test
libraryDependencies += "org.specs2" %% "specs2-core" % "4.8.1" % Test
//libraryDependencies += "org.slf4j" % "slf4j-simple" % "2.0.0-alpha1"
libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.2.3"
//libraryDependencies += "com.gu" %% "scanamo" % "1.0.0-M8"

scalacOptions ++= Seq(
  "-unchecked",
  "-deprecation",
  "-feature",
  "-Xfatal-warnings"
)

mainClass in(Compile, run) := Some("cucumber.api.cli.Main")

lazy val cucumber = taskKey[Unit]("cucumber")
cucumber := Def.taskDyn {
  val args = List("-m", "--plugin", "pretty", "--glue", "cn.easyact.fin.manager"
    , "src/test/resources")
    .mkString(" ")
  Def.task {
    (runMain in Test).toTask(s" cucumber.api.cli.Main $args").value
  }
}.value

javacOptions ++= Seq("-source", "12", "-target", "8")
scalacOptions += "-target:jvm-1.8"
//scalacOptions += "--release 9"
assemblyMergeStrategy in assembly := {
  case "module-info.class" => MergeStrategy.discard
  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
  case PathList(ps@_*) if ps.last == "Log4j2Plugins.dat" => Log4j2MergeStrategy.plugincache
  case x =>
    val oldStrategy = (assemblyMergeStrategy in assembly).value
    oldStrategy(x)
}