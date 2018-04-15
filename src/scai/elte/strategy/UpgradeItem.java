package scai.elte.strategy;

import bwapi.UpgradeType;

public class UpgradeItem extends BasePlanItem {
	
	private UpgradeType upgradeType;
	private Integer level;
	
	public Integer getLevel() {
		return level;
	}

	public void setLevel(Integer level) {
		this.level = level;
	}

	public UpgradeItem(UpgradeType type, Integer supplyThreshold, Integer importance, Integer level) {
		this.upgradeType = type;
		this.setSupplyThreshold(supplyThreshold);
		this.setImportance(importance);
		this.setLevel(level);
	}
	
	public UpgradeType getUpgradeType() {
		return upgradeType;
	}
	public void setUpgradeType(UpgradeType upgradeType) {
		this.upgradeType = upgradeType;
	}
	
	
	

}
