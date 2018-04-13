package scai.elte.strategy;

import bwapi.UpgradeType;

public class UpgradeItem extends BasePlanItem {
	
	private UpgradeType upgradeType;
	
	public UpgradeItem(UpgradeType type, Integer supplyThreshold, Integer importance) {
		this.upgradeType = type;
		this.setSupplyThreshold(supplyThreshold);
		this.setImportance(importance);
	}
	
	public UpgradeType getUpgradeType() {
		return upgradeType;
	}
	public void setUpgradeType(UpgradeType upgradeType) {
		this.upgradeType = upgradeType;
	}
	
	
	

}
