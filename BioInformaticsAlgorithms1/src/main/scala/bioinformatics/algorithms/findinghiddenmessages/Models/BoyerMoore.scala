package bioinformatics.algorithms.findinghiddenmessages.Models
import scala.annotation.tailrec
import scala.math.max


object BoyerMoore {
	
	
	def BoyerMoore(pattern: Array[Char], R: Int) {
		
		/** Search text for pattern match using bad character rule part of Boyer-More algorithm
			*
			* @constructor creates a new BoyerMoore
			* @param pattern character array
			* @param R character alphabet
			* @see [[https://algs4.cs.princeton.edu/53substring/KMP.java.html]]
			* @author Scala translation by Gary Struthers from Java by Robert Sedgewick and Kevin Wayne.
			*         https://github.com/garyaiki/Scala-Algorithms/blob/master/src/main/scala/org/gs/search/BoyerMoore.scala
			*/

		val M = pattern.length
		val right = new Array[Int](R)
		
		pattern foreach (j => right(j) = j)
		
		/** returns text offset of match, text length for no match */
		def boyerMooreSearch(text: Array[Char]): Int = {
			val N = text.length
			
			@tailrec
			def loop(i: Int): Int = {
				
				@tailrec
				def findSkip(j: Int): Int = {
					if (j < 0) 0 else if (pattern(j) != text(i + j)) max(1, j - right(text(i + j))) else findSkip(j - 1)
				}
				
				if (i <= N - M) {
					val skip = findSkip(M - 1)
					if (skip == 0) i else loop(i + skip)
				} else N
			}
			loop(0)
		}
	}
}
