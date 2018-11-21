package bioinformatics.algorithms.findinghiddenmessages.Models

object BruteForce {
	
	/*
	http://www-igm.univ-mlv.fr/~lecroq/string/node3.html#SECTION0030
	
  	void BF(char *x, int m, char *y, int n) {
   int i, j;

   /* Searching */
   for (j = 0; j <= n - m; ++j) {
      for (i = 0; i < m && x[i] == y[i + j]; ++i);
      if (i >= m)
         OUTPUT(j);
   }
}

This algorithm can be rewriting to give a more efficient algorithm in practice as follows:

#define EOS '\0'

void BF(char *x, int m, char *y, int n) {
  char *yb;
  /* Searching */
  for (yb = y; *y != EOS; ++y)
    if (memcmp(x, y, m) == 0)
      OUTPUT(y - yb);
}
	 */
}
