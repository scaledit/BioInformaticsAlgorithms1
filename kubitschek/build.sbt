
val root =
  (project in file("."))
    .configs(IntegrationTest)

name := "kubitschek"
organization := "com.ntoggle"
scalaVersion := "2.11.7"

credentials += Credentials("Artifactory Realm", "ntoggle.artifactoryonline.com", System.getenv("ARTIFACTORY_USER"), System.getenv("ARTIFACTORY_PW"))
publishArtifact in packageDoc := false // Disable publishing of docs
publishMavenStyle := true
publishTo := {
  val artifactory = "https://ntoggle.artifactoryonline.com/ntoggle/"
  if (isSnapshot.value)
    Some("libs-snapshots-local" at artifactory + "libs-snapshots-local")
  else
    Some("libs-releases-local" at artifactory + "libs-releases-local")
}

resolvers ++= Seq(
  "Typesafe Releases" at "http://repo.typesafe.com/typesafe/releases/",
  "Typesafe Snapshots" at "http://repo.typesafe.com/typesafe/snapshots/",
  "Sonatype Snapshots" at "http://oss.sonatype.org/content/repositories/snapshots/",
  "Scalaz Bintray" at "http://dl.bintray.com/scalaz/releases",
  "Twitter" at "http://maven.twttr.com",
  "nToggle Snapshots" at "https://ntoggle.artifactoryonline.com/ntoggle/libs-snapshots-local",
  "nToggle Releases" at "https://ntoggle.artifactoryonline.com/ntoggle/libs-releases-local"
  )

libraryDependencies ++= {
  val scalazVersion = "7.1.3"
  val specs2Version = "2.4.13"
  val akkaV       = "2.3.12"
  val akkaStreamV = "1.0"
  val playJsonV   = "2.4.2"
  val goldengateVersion = "0.11.0"
  val helixVersion = "0.14.0"
  val audobonVersion = "0.16.0"
  val IntegrationTestAndTest = "it;test"

  Seq(
    "com.ntoggle" %% "humber" % "0.3.0",
    "com.ntoggle" %% "albi" % "0.29.0",
    "com.ntoggle" %% "goldengate" % goldengateVersion,
    "com.ntoggle" %% "goldengate-play-json" % goldengateVersion,
    "com.ntoggle" %% "goldengate-play-json-test" % goldengateVersion % IntegrationTestAndTest,
    "com.ntoggle" %% "goldengate-slick" % goldengateVersion,
    "com.ntoggle" %% "helix-api" % helixVersion,
    "com.ntoggle" %% "helix-api-rules-etcd" % helixVersion,
    "com.ntoggle" %% "audobon-web-client" % audobonVersion,
    "com.chuusai" %% "shapeless" % "2.2.5",
    "org.scalaz" %% "scalaz-core" % scalazVersion,
    "org.scalaz" %% "scalaz-scalacheck-binding" % scalazVersion,
    "org.specs2" %% "specs2-core" % specs2Version % IntegrationTestAndTest,
    "org.specs2" %% "specs2-scalacheck" % specs2Version % IntegrationTestAndTest,
    "org.typelevel" %% "scalaz-specs2" % "0.3.0" % IntegrationTestAndTest,
    "org.scalacheck" %% "scalacheck" % "1.11.4",
    "com.typesafe.akka" %% "akka-actor" % akkaV,
    "com.typesafe.akka" %% "akka-slf4j" % akkaV,
    "com.typesafe.akka" %% "akka-stream-experimental" % akkaStreamV,
    "com.typesafe.akka" %% "akka-http-core-experimental" % akkaStreamV,
    "com.typesafe.akka" %% "akka-http-experimental" % akkaStreamV,
    "com.typesafe.akka" %% "akka-http-spray-json-experimental" % akkaStreamV,
    "com.typesafe.akka" %% "akka-http-testkit-experimental" % akkaStreamV,
    "com.typesafe.play" %% "play-json" % playJsonV,
    "com.typesafe.slick" %% "slick" % "2.1.0",
    "com.h2database" % "h2" % "1.4.188" % IntegrationTestAndTest,
    "org.postgresql" % "postgresql" % "9.4-1201-jdbc41",
    "com.typesafe.slick" %% "slick" % "2.1.0",
    "com.github.tminglei" %% "slick-pg" % "0.8.5",
    "ch.qos.logback" % "logback-classic" % "1.1.2",
    "com.typesafe.scala-logging" %% "scala-logging-slf4j" % "2.1.2",
    "org.slf4j" % "jcl-over-slf4j" % "1.7.12",  // some of our libs use commons-logging, this redirect to logback
    "de.heikoseeberger" %% "akka-http-play-json" % "1.0.0"
  )
}

scalacOptions ++= Seq(
  "-language:postfixOps",
  "-deprecation",
  "-feature",
  "-language:implicitConversions",
  "-language:existentials",
  "-language:higherKinds",
  "-encoding", "UTF-8",
  "-unchecked",
  "-Xfatal-warnings",
  "-Xlint",
  "-Yno-adapted-args",
  "-Ywarn-dead-code",
  "-Ywarn-value-discard"
)

assemblyJarName in assembly := "kubitschek.jar"

assemblyMergeStrategy in assembly := {
//  case PathList("org", "joda", "convert", xs@_*) => MergeStrategy.last
//  case PathList("org", "apache", "commons", "logging", xs@_*) => MergeStrategy.last
  case PathList("META-INF", "io.netty.versions.properties") =>
    MergeStrategy.discard
  case PathList("reference.conf") => MergeStrategy.concat //allow libraries to include their configs...
  case PathList(p) if p.endsWith(".conf") => MergeStrategy.discard // exclude our conf files
  case PathList("logback.xml") => MergeStrategy.discard // exclude logback.xml if it sneaks in
  case x =>
    val oldStrategy = (assemblyMergeStrategy in assembly).value
    oldStrategy(x)
}


//Show full stack trace and duration in ScalaTest failures
testOptions in Test += Tests.Argument(TestFrameworks.ScalaTest, "-oFD")
testOptions in IntegrationTest += Tests.Argument(TestFrameworks.ScalaTest, "-oFD")

//Show full stack trace and duration in Specs2 failures
testOptions in Test += Tests.Argument(TestFrameworks.Specs2, "failtrace=true", "fullstacktrace")
testOptions in IntegrationTest += Tests.Argument(TestFrameworks.Specs2, "failtrace=true", "fullstacktrace")

fork := true
parallelExecution in Test := true
javaOptions in Test ++= Seq("-Dfile.encoding=UTF8", "-Xmx2G")
javaOptions ++= Seq("-Dfile.encoding=UTF8")
Defaults.itSettings

initialCommands in console := "import scalaz._, Scalaz._"

addCommandAlias("runJk", "run -c src/main/resources/kubitschek.conf")
