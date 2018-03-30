package scai.elte.strategy;

import java.util.ArrayList;

import bwapi.UnitType;

public class BuildOrder {
	
	public ArrayList<BuildOrderItem> buildOrderList = new ArrayList<BuildOrderItem>();
	
	public void addItem(UnitType ut, Integer supply, Integer importance) {
		buildOrderList.add(new BuildOrderItem(ut, supply, importance, BuildOrderItemStatus.PLANNED));
	}
	
	public void addItem(UnitType ut, Integer supply, Integer importance, BuildOrderItemStatus status) {
		buildOrderList.add(new BuildOrderItem(ut, supply, importance, status));
	}

}
