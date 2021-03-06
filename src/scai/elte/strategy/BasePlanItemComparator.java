package scai.elte.strategy;

import java.util.Comparator;

public class BasePlanItemComparator implements Comparator<BasePlanItem>
	{
	    @Override
	    public int compare(BasePlanItem x, BasePlanItem y)
	    {
	        if (x.getImportance() > y.getImportance())
	        {
	            return -1;
	        }

	        if (x.getImportance() < y.getImportance())
	        {
	            return 1;
	        }
			return 0;
	    }
	}