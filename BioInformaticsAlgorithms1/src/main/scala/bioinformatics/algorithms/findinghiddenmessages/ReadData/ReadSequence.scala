package bioinformatics.algorithms.findinghiddenmessages.ReadData

import org.apache.spark.sql.SparkSession

import org.apache.spark.sql.types._

object ReadSequence {
	
	def readSingleSequence():Unit = {
	val logFile = "/Users/smysore/Downloads/bactexample.csv" // Should be some file on your system
	val spark = SparkSession.builder
				.appName("DNAsa")
  		.master("local[4]")
				.getOrCreate()
		//set new runtime options
		spark.conf.set("spark.sql.shuffle.partitions", 6)
		spark.conf.set("spark.executor.memory", "2g")
		//get all settings
		val configMap:Map[String, String] = spark.conf.getAll
		import spark.implicits._
		
	val logData = spark.read.textFile(logFile).cache()
	
		val alpha = logData.flatMap(line => line.split("") )
		
		alpha.rdd.map(char => (char,1)).reduceByKey(_+_).foreach(println)
	
	
	spark.stop()
	}
	
}
