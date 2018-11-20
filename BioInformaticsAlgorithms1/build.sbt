name := "BioInformaticsAlgorithms1"

version := "0.1"

scalaVersion := "2.12.7"

// https://mvnrepository.com/artifact/org.apache.spark/spark-core
libraryDependencies ++= Seq("org.apache.spark" %% "spark-core" % "2.4.0"
	, "org.apache.spark" %% "spark-sql" % "2.4.0"
	, "org.apache.spark" %% "spark-mllib" % "2.4.0"
	, "org.apache.spark" %% "spark-streaming" % "2.4.0"
)
