package scai.elte.strategy;

import bwapi.UpgradeType;

public class UpgradeItem {
	
	private UpgradeType upgradeType;
	private Integer supplyThreshold;
	private Integer priority;
	
	public UpgradeItem(UpgradeType type, Integer threshold, Integer priority) {
		this.upgradeType = type;
		this.supplyThreshold = threshold;
		this.priority=priority;
	}
	
	public UpgradeType getUpgradeType() {
		return upgradeType;
	}
	public void setUpgradeType(UpgradeType upgradeType) {
		this.upgradeType = upgradeType;
	}
	public Integer getSupplyThreshold() {
		return supplyThreshold;
	}
	public void setSupplyThreshold(Integer supplyThreshold) {
		this.supplyThreshold = supplyThreshold;
	}

	public Integer getPriority() {
		return priority;
	}

	public void setPriority(Integer priority) {
		this.priority = priority;
	}
	
	
	

}
