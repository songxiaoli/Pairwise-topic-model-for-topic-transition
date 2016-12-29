package Model;

/**
* @author  Xiaoli Song
*/

public class ProbHolder implements Comparable<ProbHolder> {
    public final Object obj;
    public final double prob;
   
    public ProbHolder(Object k, double v){
        this.obj = k;
        this.prob = v;       
    }
   
    public int compareTo(ProbHolder p){
        if (this.prob == p.prob)
        	return 0;
        else if (this.prob < p.prob)
        	return 1;
        else
        	return -1;
    }
}