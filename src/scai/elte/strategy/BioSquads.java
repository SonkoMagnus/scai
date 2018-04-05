package scai.elte.strategy;

import bwapi.UnitType;

public class BioSquads extends BuildOrder {

	public BioSquads() {
		super();
		//derp
		addItem(UnitType.Terran_Refinery, 12, 1);
		//derp
		addItem(UnitType.Terran_Supply_Depot, 18, 1);
		
		addItem(UnitType.Terran_Barracks, 22, 1);
		addItem(UnitType.Terran_Bunker, 26, 1);
		
		addItem(UnitType.Terran_Supply_Depot, 28, 1);
		addItem(UnitType.Terran_Bunker, 28, 1);
		
		addItem(UnitType.Terran_Refinery, 36, 1);
		addItem(UnitType.Terran_Academy, 38, 1);
		addItem(UnitType.Terran_Barracks, 48, 1);
	}
	
}
