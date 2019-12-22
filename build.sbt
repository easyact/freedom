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
//  "org.scala-lang" % "scala-library" % "2.12.10",
  "org.scalaz" % "scalaz-concurrent_2.12" % "7.3.0-M27",
  "com.typesafe.scala-logging" % "scala-logging_2.12" % "3.9.2"
)

scalacOptions ++= Seq(
  "-unchecked",
  "-deprecation",
  "-feature",
  "-Xfatal-warnings"
)

assemblyMergeStrategy in assembly := {
  case PathList(ps@_*) if ps.last == "Log4j2Plugins.dat" =>
    Log4j2MergeStrategy.plugincache
  case x =>
    val oldStrategy = (assemblyMergeStrategy in assembly).value
    oldStrategy(x)
}
