package scai.elte.strategy;

import bwapi.TilePosition;
import bwapi.UnitType;


//Basically 2 rax FE, then marine+medic spam
public class BioSquads extends BuildOrder {
	
	private TilePosition natural;

	public TilePosition getNatural() {
		return natural;
	}

	public void setNatural(TilePosition natural) {
		this.natural = natural;
	}
	

	public BioSquads(TilePosition natural) {
		super();
		
		addItem(UnitType.Terran_Supply_Depot, 18, 1);	

		
		addItem(UnitType.Terran_Barracks, 22, 1);
		addItem(UnitType.Terran_Supply_Depot, 28, 1);
		addItem(UnitType.Terran_Barracks, 24, 1);
		addItem(UnitType.Terran_Refinery, 24, 1);
		
		addItem(UnitType.Terran_Command_Center, 28, 1, natural);

		/*
		addItem(UnitType.Terran_Supply_Depot, 32, 1);
		addItem(UnitType.Terran_Refinery, 36, 1);
		addItem(UnitType.Terran_Academy, 38, 1);
		addItem(UnitType.Terran_Barracks, 48, 1);
		*/
		setSupplyExecuted(28);
	}
	
}
