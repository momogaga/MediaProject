package fr.mediastore.hadoop;

import java.util.Comparator;

class RecordComparator implements Comparator<ListElem>
{
	public int compare(ListElem o1, ListElem o2) 
	{
		double ret = 0;

		double dist = o1.getDist() - o2.getDist();
		if (Math.abs(dist) < 1E-6) {
			//ret = dist;
			ret = (o1.getId()).compareTo(o2.getId());	
		} else if (dist > 0)
			ret = 1;
		else if (dist < 0)
			ret = -1;

		return (int)-ret;  //Descending order
	}
}
