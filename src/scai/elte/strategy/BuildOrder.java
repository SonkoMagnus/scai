package scai.elte.strategy;

import java.util.ArrayList;

import bwapi.UnitType;

public class BuildOrder {
	
	public ArrayList<BuildOrderItem> buildOrderList = new ArrayList<BuildOrderItem>();
	private int supplyExecuted; //When all the items are done.
	
	public int getSupplyExecuted() {
		return supplyExecuted;
	}

	public void setSupplyExecuted(int supplyExecuted) {
		this.supplyExecuted = supplyExecuted;
	}

	public void addItem(UnitType ut, Integer supply, Integer importance) {
		buildOrderList.add(new BuildOrderItem(ut, supply, importance, BuildOrderItemStatus.PLANNED));
	}
	
	public void addItem(UnitType ut, Integer supply, Integer importance, BuildOrderItemStatus status) {
		buildOrderList.add(new BuildOrderItem(ut, supply, importance, status));
	}

}
