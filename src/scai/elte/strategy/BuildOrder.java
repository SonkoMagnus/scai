package scai.elte.strategy;

import java.util.ArrayList;

import bwapi.TilePosition;
import bwapi.UnitType;

public class BuildOrder {
	
	public ArrayList<BuildOrderItem> buildOrderList = new ArrayList<BuildOrderItem>();
	private int supplyExecuted; //When all the items are done.
	private ArrayList<UpgradeItem> upgradeOrder = new ArrayList<UpgradeItem>(); 
	private ArrayList<TechItem> techOrder = new ArrayList<TechItem>(); 

	
	public int getSupplyExecuted() {
		return supplyExecuted;
	}

	public void setSupplyExecuted(int supplyExecuted) {
		this.supplyExecuted = supplyExecuted;
	}

	public void addItem(UnitType ut, Integer supply, Integer importance) {
		buildOrderList.add(new BuildOrderItem(ut, supply, importance, BuildOrderItemStatus.PLANNED));
	}
	
	
	public void addItem(UnitType ut, Integer supply, Integer importance, TilePosition position) {
		buildOrderList.add(new BuildOrderItem(ut, supply, importance, BuildOrderItemStatus.PLANNED, position));
	}

	public ArrayList<UpgradeItem> getUpgradeOrder() {
		return upgradeOrder;
	}

	public void setUpgradeOrder(ArrayList<UpgradeItem> upgradeOrder) {
		this.upgradeOrder = upgradeOrder;
	}

	public ArrayList<TechItem> getTechOrder() {
		return techOrder;
	}

	public void setTechOrder(ArrayList<TechItem> techOrder) {
		this.techOrder = techOrder;
	}
	

}
