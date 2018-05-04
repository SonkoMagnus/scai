package scai.elte.main;

import java.util.Comparator;

public class ScoutInfoComparator implements Comparator<ScoutInfo> {
	
    @Override
    public int compare(ScoutInfo x, ScoutInfo y)
    {
        if (x.getImportance() >= y.getImportance())
        {
            return -1;
        }

        else if (x.getImportance() < y.getImportance())
        {
            return 1;
        }
		return 0;
    }

}