package scai.elte.strategy;

import java.util.UUID;

import bwapi.TilePosition;
import bwapi.UnitType;

public class BuildOrderItem {
	
	private UUID id;

	private UnitType unitType;
	private Integer supplyThreshold;
	
	private TilePosition tilePosition;

	public BuildOrderItemStatus status;
	public boolean gotBuilder = false;
	/**
	 * If two items should be built at the same time, the one with the higher importance gets built
	 */
	
	private Integer importance;
	/*
	public BuildOrderItem(UnitType unitType,  Integer supplyThreshold,Integer importance) {
		this.unitType=unitType;
		this.supplyThreshold=supplyThreshold;
		this.importance=importance;
	}*/
	
	
	public BuildOrderItem(UnitType unitType,  Integer supplyThreshold,Integer importance, BuildOrderItemStatus status) {
		this.unitType=unitType;
		this.supplyThreshold=supplyThreshold;
		this.importance=importance;
		this.status=status;
		this.id=UUID.randomUUID();
	}

	public UnitType getUnitType() {
		return unitType;
	}

	public void setUnitType(UnitType unitType) {
		this.unitType = unitType;
	}

	public Integer getSupplyThreshold() {
		return supplyThreshold;
	}

	public void setSupplyThreshold(Integer supplyThreshold) {
		this.supplyThreshold = supplyThreshold;
	}

	public Integer getImportance() {
		return importance;
	}

	public void setImportance(Integer importance) {
		this.importance = importance;
	}

	public TilePosition getTilePosition() {
		return tilePosition;
	}

	public void setTilePosition(TilePosition tilePosition) {
		this.tilePosition = tilePosition;
	}
	



}
