package bioinformatics.algorithms.findinghiddenmessages.ReadData

import org.apache.spark.sql.SparkSession

object ReadSequence {
	
	def readSingleSequence():Unit = {
	val logFile = "/Users/smysore/Downloads/grch38p2.csv" // Should be some file on your system
	val spark = SparkSession.builder
				.appName("DNAsa")
  		.master("local[4]")
				.getOrCreate()
		//set new runtime options
		spark.conf.set("spark.sql.shuffle.partitions", 6)
		spark.conf.set("spark.executor.memory", "2g")
		//get all settings
		val configMap:Map[String, String] = spark.conf.getAll
		
	val logData = spark.read.textFile(logFile).cache()
	val numAs = logData.filter(line => line.contains("a")).count()
	val numBs = logData.filter(line => line.contains("b")).count()
	println(s"Lines with a: $numAs, Lines with b: $numBs")
	
	spark.stop()
	}
	
}
