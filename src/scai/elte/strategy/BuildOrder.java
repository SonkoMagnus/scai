package scai.elte.strategy;

import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;

import bwapi.TilePosition;
import bwapi.UnitType;

public class BuildOrder {
	
	public ArrayList<BuildOrderItem> buildOrderList = new ArrayList<BuildOrderItem>();
	private int supplyExecuted; //When all the items are done.
	private CopyOnWriteArrayList<BasePlanItem> improveOrder = new CopyOnWriteArrayList<BasePlanItem>();
	
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

	public CopyOnWriteArrayList<BasePlanItem> getImproveOrder() {
		return improveOrder;
	}

	public void setImproveOrder(CopyOnWriteArrayList<BasePlanItem> improveOrder) {
		this.improveOrder = improveOrder;
	}




}
