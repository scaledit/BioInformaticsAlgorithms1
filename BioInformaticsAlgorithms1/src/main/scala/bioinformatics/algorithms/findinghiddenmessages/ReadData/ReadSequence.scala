package bioinformatics.algorithms.findinghiddenmessages.ReadData

import org.apache.spark.sql.SparkSession
import bioinformatics.algorithms.findinghiddenmessages.Models.BoyerMoore

object ReadSequence {
	
	def readSingleSequence():Unit = {
	val logFile = "/Users/smysore/Downloads/bactexample.csv" // Should be some file on your system
	val spark = SparkSession.builder
				.appName("DNAsa")
  		.master("local[4]")
				.getOrCreate()
		//set new runtime options
		spark.conf.set("spark.sql.shuffle.partitions", 4)
		spark.conf.set("spark.executor.memory", "4g")
		//get all settings
		
		import spark.implicits._
		
	val alphaDF = spark.read.textFile(logFile).cache()
	
		for (windowSize <- 1 to 3) yield {
		val ngrams = alphaDF.mapPartitions(
			_.toList.mkString("")
			.replace("\n","")
			.replace(" ","")
			.sliding(windowSize)
		)
		
			val charArray = ngrams.toString.toSeq.toArray
			
			BoyerMoore(charArray,windowSize)
			
		
		
		val nGramCount = ngrams.rdd.map(ng => (ng,1)).reduceByKey(_+_)
		
		nGramCount.coalesce(1,true)
			.saveAsTextFile(s"/Users/smysore/Downloads/ngram_$windowSize")
		}
		
		spark.stop()
	}
	
}
