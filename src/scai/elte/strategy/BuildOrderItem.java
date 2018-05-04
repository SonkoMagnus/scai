package scai.elte.strategy;

import bwapi.TilePosition;
import bwapi.UnitType;

public class BuildOrderItem extends BasePlanItem {

	private UnitType unitType;
	private TilePosition tilePosition;
	public BuildOrderItemStatus status;
	
	public BuildOrderItem(UnitType unitType,  Integer supplyThreshold,Integer importance, BuildOrderItemStatus status) {
		this.unitType=unitType;
		this.setSupplyThreshold(supplyThreshold);
		this.setImportance(importance);
		this.status=status;
	}
	
	public BuildOrderItem(UnitType unitType,  Integer supplyThreshold,Integer importance, BuildOrderItemStatus status, TilePosition position) {
		this.unitType=unitType;
		this.setSupplyThreshold(supplyThreshold);
		this.setImportance(importance);
		this.status=status;
		this.tilePosition=position;
	}

	public UnitType getUnitType() {
		return unitType;
	}

	public void setUnitType(UnitType unitType) {
		this.unitType = unitType;
	}

	public TilePosition getTilePosition() {
		return tilePosition;
	}

	public void setTilePosition(TilePosition tilePosition) {
		this.tilePosition = tilePosition;
	}
	



}
