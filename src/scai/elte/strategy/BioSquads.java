package scai.elte.strategy;

import bwapi.UnitType;

public class BioSquads extends BuildOrder {

	public BioSquads() {
		super();
		
		addItem(UnitType.Terran_Supply_Depot, 18, 1);
		
		addItem(UnitType.Terran_Barracks, 22, 1);
		addItem(UnitType.Terran_Supply_Depot, 28, 1);
		addItem(UnitType.Terran_Barracks, 24, 1);

		/*
		addItem(UnitType.Terran_Supply_Depot, 32, 1);
		addItem(UnitType.Terran_Refinery, 36, 1);
		addItem(UnitType.Terran_Academy, 38, 1);
		addItem(UnitType.Terran_Barracks, 48, 1);
		*/
		setSupplyExecuted(28);
	}
	
}
