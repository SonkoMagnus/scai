package scai.elte.strategy;

import bwapi.TechType;
import bwapi.UpgradeType;

public class TechItem extends BasePlanItem{
	
	private TechType techType;
	
	public TechItem(TechType techType, Integer supplyThreshold,Integer importance ) {
		this.techType = techType;
		this.setSupplyThreshold(supplyThreshold);
		this.setImportance(importance);
	}

	public TechType getTechType() {
		return techType;
	}
	public void setTechType(TechType techType) {
		this.techType = techType;
	}
}
