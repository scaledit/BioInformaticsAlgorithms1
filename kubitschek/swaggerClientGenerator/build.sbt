lazy val genAngularClient = taskKey[Unit]("Execute the shell script")

genAngularClient := {
  "node angularClient.js" ! ; "cp client-package.json src/main/public/js/package.json" !
}

lazy val genNodeClient = taskKey[Unit]("Execute the shell script")

genNodeClient := {
  "node nodeClient.js" ! ; "cp client-package.json src/main/public/js/package.json" !
}

val root =
  (project in file("."))
    .configs(IntegrationTest)

/*
lazy val util = (project in file(".")).
enablePlugins(SbtWeb).
settings(
name := "jk-utils"
)
*/

name := "kubitschek-angular-api-client"

organization := "com.ntoggle"

scalaVersion := "2.11.7"

credentials += Credentials("Artifactory Realm", "ntoggle.artifactoryonline.com", System.getenv("ARTIFACTORY_USER"), System.getenv("ARTIFACTORY_PW"))

publishMavenStyle := true

publishTo := {
  val artifactory = "https://ntoggle.artifactoryonline.com/ntoggle/"
  if (isSnapshot.value)
    Some("libs-snapshots-local" at artifactory + "libs-snapshots-local")
  else
    Some("libs-releases-local" at artifactory + "libs-releases-local")
}

fork := true

resolvers ++= Seq(
  "Typesafe Releases" at "http://repo.typesafe.com/typesafe/releases/",
  "Typesafe Snapshots" at "http://repo.typesafe.com/typesafe/snapshots/",
  "Sonatype Snapshots" at "http://oss.sonatype.org/content/repositories/snapshots/",
  "Scalaz Bintray" at "http://dl.bintray.com/scalaz/releases",
  "Twitter" at "http://maven.twttr.com",
  "nToggle Snapshots" at "https://ntoggle.artifactoryonline.com/ntoggle/libs-snapshots-local",
  "nToggle Releases" at "https://ntoggle.artifactoryonline.com/ntoggle/libs-releases-local"
  )

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

//Show full stack trace and duration in ScalaTest failures
testOptions in Test += Tests.Argument(TestFrameworks.ScalaTest, "-oFD")

//Show full stack trace and duration in Specs2 failures
testOptions in Test += Tests.Argument(TestFrameworks.Specs2, "failtrace=true", "fullstacktrace")

parallelExecution in Test := true

publishArtifact in packageDoc := false // Disable publishing of docs

//WebKeys.packagePrefix in Assets := "public/"

//mappings in (Compile, packageBin) <+= baseDirectory map { base =>
//   (base / "RulesApiSwaggerAngularClient.js") -> "src/main/public/js/example.txt"
//}

