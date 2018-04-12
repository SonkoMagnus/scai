package scai.elte.strategy;

import bwapi.TechType;
import bwapi.UpgradeType;

public class TechItem {
	
	private TechType techType;
	private Integer supplyThreshold;
	private Integer priority;
	
	public TechItem(TechType techType, Integer supplyThreshold,Integer priority ) {
		this.techType = techType;
		this.supplyThreshold = supplyThreshold;
		this.priority=priority;
	}
	
	
	public Integer getSupplyThreshold() {
		return supplyThreshold;
	}
	public void setSupplyThreshold(Integer supplyThreshold) {
		this.supplyThreshold = supplyThreshold;
	}
	public TechType getTechType() {
		return techType;
	}
	public void setTechType(TechType techType) {
		this.techType = techType;
	}


	public Integer getPriority() {
		return priority;
	}


	public void setPriority(Integer priority) {
		this.priority = priority;
	}

}
