package scai.elte.strategy;

import bwapi.UnitType;

public class BioSquads extends BuildOrder {

	public BioSquads() {
		super();
		addItem(UnitType.Terran_Supply_Depot, 16, 1);
		addItem(UnitType.Terran_Barracks, 18, 1);
		addItem(UnitType.Terran_Bunker, 20, 1);
		addItem(UnitType.Terran_Supply_Depot, 32, 1);
		addItem(UnitType.Terran_Academy, 36, 1);
		addItem(UnitType.Terran_Barracks, 38, 1);
	}
	
}
