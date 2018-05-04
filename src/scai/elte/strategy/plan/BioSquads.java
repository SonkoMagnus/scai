package scai.elte.strategy.plan;

import bwapi.TilePosition;
import bwapi.UnitType;
import scai.elte.strategy.BuildOrder;


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
		setSupplyExecuted(28);
	}
	
}
